<?php

namespace App\Http\Controllers\Quran;

use App\Http\Controllers\Controller;
use App\Http\Requests\Quran\SyncWirdEntriesRequest;
use App\Models\WirdEntry;
use Carbon\CarbonImmutable;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class WirdController extends Controller
{
    /** GET /api/quran/wirds?since=ISO8601 — entries changed since `since` (including deletions). */
    public function index(Request $request): JsonResponse
    {
        $request->validate(['since' => ['sometimes', 'date']]);
        $serverTime = now();

        $query = WirdEntry::withTrashed()->where('user_id', $request->user()->id);
        if ($request->filled('since')) {
            $query->where('updated_at', '>', CarbonImmutable::parse($request->input('since'))->setTimezone(config('app.timezone')));
        }

        return response()->json([
            'entries' => $query->orderBy('updated_at')->limit(1000)->get()->map->toSyncArray()->values(),
            'server_time' => $serverTime->toIso8601String(),
        ]);
    }

    /** POST /api/quran/wirds — upsert by client_id; `deleted: true` soft-deletes. Safe to resend. */
    public function sync(SyncWirdEntriesRequest $request): JsonResponse
    {
        $userId = $request->user()->id;

        $saved = DB::transaction(function () use ($request, $userId) {
            return collect($request->validated('entries'))->map(function (array $data) use ($userId) {
                $entry = WirdEntry::withTrashed()->firstOrNew([
                    'user_id' => $userId,
                    'client_id' => $data['client_id'],
                ]);
                $entry->fill([
                    'from_ayah_id' => $data['from_ayah_id'],
                    'to_ayah_id' => $data['to_ayah_id'],
                    // Store in the app timezone: Eloquent writes Carbon values without converting them.
                    'read_at' => CarbonImmutable::parse($data['read_at'])->setTimezone(config('app.timezone')),
                ]);
                $entry->save();

                if (($data['deleted'] ?? false) && ! $entry->trashed()) {
                    $entry->delete();
                } elseif (! ($data['deleted'] ?? false) && $entry->trashed()) {
                    $entry->restore();
                }

                return $entry->refresh()->toSyncArray();
            });
        });

        return response()->json(['entries' => $saved->values()]);
    }

    /** GET /api/quran/wirds/stats?tz=Asia/Riyadh — today, last 7 days, streak, next ayah. */
    public function stats(Request $request): JsonResponse
    {
        $request->validate(['tz' => ['sometimes', 'timezone:all']]);
        $tz = $request->input('tz', 'Asia/Riyadh');
        $today = CarbonImmutable::now($tz)->startOfDay();

        $entries = WirdEntry::where('user_id', $request->user()->id)
            ->where('read_at', '>=', $today->subDays(365)->setTimezone(config('app.timezone')))
            ->get(['from_ayah_id', 'to_ayah_id', 'read_at']);

        $count = fn ($items) => $items->sum(fn (WirdEntry $e) => $e->ayahCount());
        $local = fn (WirdEntry $e) => CarbonImmutable::parse($e->read_at)->setTimezone($tz);

        $days = $entries->map(fn ($e) => $local($e)->toDateString())->unique()->flip();
        $cursor = $days->has($today->toDateString()) ? $today : $today->subDay();
        $streak = 0;
        while ($days->has($cursor->toDateString())) {
            $streak++;
            $cursor = $cursor->subDay();
        }

        $last = WirdEntry::where('user_id', $request->user()->id)->latest('read_at')->first();

        return response()->json([
            'today_ayahs' => $count($entries->filter(fn ($e) => $local($e)->greaterThanOrEqualTo($today))),
            'week_ayahs' => $count($entries->filter(fn ($e) => $local($e)->greaterThanOrEqualTo($today->subDays(6)))),
            'streak_days' => $streak,
            'next_ayah_id' => $last ? min(SyncWirdEntriesRequest::MAX_AYAH_ID, $last->to_ayah_id + 1) : null,
        ]);
    }
}
