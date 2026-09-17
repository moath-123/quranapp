# مقترح واجهة Laravel لمزامنة الورد والمفضلة

> **مقترح فقط:** كود تعاهد غير متوفر لنا. الهدف أن تكون بيانات الورد والمفضلة مع حساب المستخدم وتتزامن بين أجهزته، والـSDK لا تخزّن شيئاً (قرار D5).

## المبادئ
- **الآيات تُعرَّف بـ`ayah_id`** (1–6236، ترتيب المصحف) لا بأرقام الصفحات، فتبقى صحيحة مع تغيّر الطبعة (Q5).
- **الورد نطاق** `from_ayah_id` ← `to_ayah_id` مع وقت القراءة. عدد الآيات = `to - from + 1`.
- **التزامن بسيط:** كل سجل له `client_id` (UUID يولّده الجهاز)، وإعادة الإرسال لا تكرر السجل.
- **الحذف ناعم:** `deleted_at`، حتى تصل عمليات الحذف لبقية الأجهزة.

## الجداول (migrations)
```php
Schema::create('wird_entries', function (Blueprint $t) {
    $t->id();
    $t->foreignId('user_id')->constrained()->cascadeOnDelete();
    $t->uuid('client_id');
    $t->unsignedSmallInteger('from_ayah_id');   // 1..6236
    $t->unsignedSmallInteger('to_ayah_id');     // >= from
    $t->timestamp('read_at');
    $t->timestamps();
    $t->softDeletes();
    $t->unique(['user_id', 'client_id']);
    $t->index(['user_id', 'read_at']);
});

Schema::create('ayah_bookmarks', function (Blueprint $t) {
    $t->id();
    $t->foreignId('user_id')->constrained()->cascadeOnDelete();
    $t->unsignedSmallInteger('ayah_id');
    $t->timestamps();
    $t->softDeletes();
    $t->unique(['user_id', 'ayah_id']);
});

Schema::create('reading_positions', function (Blueprint $t) {
    $t->foreignId('user_id')->primary()->constrained()->cascadeOnDelete();
    $t->unsignedSmallInteger('ayah_id');
    $t->unsignedSmallInteger('page');           // للعرض فقط
    $t->string('edition', 20)->default('madani-1405');
    $t->timestamps();
});
```

## التحقق (FormRequest)
```php
'entries' => 'required|array|max:200',
'entries.*.client_id' => 'required|uuid',
'entries.*.from_ayah_id' => 'required|integer|between:1,6236',
'entries.*.to_ayah_id' => 'required|integer|between:1,6236|gte:entries.*.from_ayah_id',
'entries.*.read_at' => 'required|date|before_or_equal:now',
'entries.*.deleted' => 'sometimes|boolean',
```

## الواجهة (OpenAPI 3.1)
```yaml
openapi: 3.1.0
info: { title: Taahud Quran Sync, version: 0.1.0 }
security: [{ sanctum: [] }]
paths:
  /api/quran/wirds:
    get:
      summary: سجلات الورد المتغيرة منذ وقت معيّن (للمزامنة)
      parameters:
        - { name: since, in: query, schema: { type: string, format: date-time } }
      responses:
        "200":
          content:
            application/json:
              schema:
                type: object
                properties:
                  entries: { type: array, items: { $ref: "#/components/schemas/WirdEntry" } }
                  server_time: { type: string, format: date-time }
    post:
      summary: رفع سجلات جديدة/معدّلة/محذوفة (upsert بـ client_id)
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [entries]
              properties:
                entries: { type: array, maxItems: 200, items: { $ref: "#/components/schemas/WirdEntryInput" } }
      responses:
        "200": { description: تم، ويُرجع السجلات بعد الحفظ }
        "422": { description: خطأ تحقق }
  /api/quran/wirds/stats:
    get:
      summary: إحصائيات (اليوم، الأسبوع، الأيام المتتالية) حسب منطقة المستخدم الزمنية
      parameters:
        - { name: tz, in: query, schema: { type: string, example: Asia/Riyadh } }
      responses:
        "200":
          content:
            application/json:
              schema:
                type: object
                properties:
                  today_ayahs: { type: integer }
                  week_ayahs: { type: integer }
                  streak_days: { type: integer }
                  next_ayah_id: { type: integer, description: الآية التالية لآخر ورد }
  /api/quran/bookmarks:
    get: { summary: المفضلة (مع since للمزامنة) }
    put:
      summary: إضافة/إزالة
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                add: { type: array, items: { type: integer, minimum: 1, maximum: 6236 } }
                remove: { type: array, items: { type: integer, minimum: 1, maximum: 6236 } }
  /api/quran/position:
    get: { summary: آخر موضع قراءة }
    put: { summary: تحديث آخر موضع قراءة }
  /api/app/flags:
    get:
      summary: مفاتيح التشغيل (مثل new_mushaf) — للإطلاق التدريجي والرجوع الفوري
components:
  securitySchemes:
    sanctum: { type: http, scheme: bearer }
  schemas:
    WirdEntryInput:
      type: object
      required: [client_id, from_ayah_id, to_ayah_id, read_at]
      properties:
        client_id: { type: string, format: uuid }
        from_ayah_id: { type: integer, minimum: 1, maximum: 6236 }
        to_ayah_id: { type: integer, minimum: 1, maximum: 6236 }
        read_at: { type: string, format: date-time }
        deleted: { type: boolean, default: false }
    WirdEntry:
      allOf:
        - $ref: "#/components/schemas/WirdEntryInput"
        - type: object
          properties:
            ayah_count: { type: integer }
            updated_at: { type: string, format: date-time }
```

## حساب الإحصائيات (مثال)
```php
$tz = $request->query('tz', 'Asia/Riyadh');
$today = now($tz)->startOfDay();
$entries = $user->wirdEntries()->where('read_at', '>=', $today->copy()->subDays(6))->get();
$todayAyahs = $entries->where('read_at', '>=', $today)->sum(fn ($e) => $e->to_ayah_id - $e->from_ayah_id + 1);
// الأيام المتتالية: أيام فريدة (بمنطقة المستخدم) تنتهي باليوم أو بالأمس
```

## أسئلة لفريق تعاهد
- **المصادقة:** هل هي Sanctum أم Passport أم غيرها؟
- **الورد:** هل يوجد مفهوم «ورد» قائم في تعاهد يجب أن يتوافق معه هذا التصميم (مثل الختمات والأهداف اليومية)؟
