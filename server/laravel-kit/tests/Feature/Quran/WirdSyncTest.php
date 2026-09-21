<?php

namespace Tests\Feature\Quran;

use App\Models\User;
use App\Models\WirdEntry;
use Carbon\CarbonImmutable;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class WirdSyncTest extends TestCase
{
    use RefreshDatabase;

    private function entry(array $overrides = []): array
    {
        return array_merge([
            'client_id' => (string) Str::uuid(),
            'from_ayah_id' => 8,
            'to_ayah_id' => 20,
            'read_at' => now()->subHour()->toIso8601String(),
        ], $overrides);
    }

    public function test_requires_authentication(): void
    {
        $this->getJson('/api/quran/wirds')->assertUnauthorized();
        $this->postJson('/api/quran/wirds', ['entries' => [$this->entry()]])->assertUnauthorized();
    }

    public function test_upsert_is_idempotent_and_reports_ayah_count(): void
    {
        $user = User::factory()->create();
        Sanctum::actingAs($user);
        $entry = $this->entry();

        $this->postJson('/api/quran/wirds', ['entries' => [$entry]])
            ->assertOk()
            ->assertJsonPath('entries.0.ayah_count', 13)
            ->assertJsonPath('entries.0.deleted', false);
        $this->postJson('/api/quran/wirds', ['entries' => [$entry]])->assertOk();

        $this->assertSame(1, WirdEntry::where('user_id', $user->id)->count());
    }

    public function test_validation(): void
    {
        Sanctum::actingAs(User::factory()->create());

        $this->postJson('/api/quran/wirds', ['entries' => [$this->entry(['from_ayah_id' => 30, 'to_ayah_id' => 10])]])
            ->assertUnprocessable()->assertJsonValidationErrors('entries.0.to_ayah_id');
        $this->postJson('/api/quran/wirds', ['entries' => [$this->entry(['to_ayah_id' => 6237])]])
            ->assertUnprocessable()->assertJsonValidationErrors('entries.0.to_ayah_id');
        $this->postJson('/api/quran/wirds', ['entries' => [$this->entry(['client_id' => 'not-a-uuid'])]])
            ->assertUnprocessable()->assertJsonValidationErrors('entries.0.client_id');
        $this->postJson('/api/quran/wirds', ['entries' => []])
            ->assertUnprocessable()->assertJsonValidationErrors('entries');
    }

    public function test_delete_and_restore_are_synced_to_other_devices(): void
    {
        Sanctum::actingAs(User::factory()->create());
        $entry = $this->entry();
        $this->postJson('/api/quran/wirds', ['entries' => [$entry]])->assertOk();
        $since = now()->subSecond()->toIso8601String();

        $this->travel(2)->seconds();
        $this->postJson('/api/quran/wirds', ['entries' => [array_merge($entry, ['deleted' => true])]])
            ->assertOk()->assertJsonPath('entries.0.deleted', true);

        $this->getJson('/api/quran/wirds?since='.urlencode($since))
            ->assertOk()
            ->assertJsonCount(1, 'entries')
            ->assertJsonPath('entries.0.client_id', $entry['client_id'])
            ->assertJsonPath('entries.0.deleted', true);

        $this->postJson('/api/quran/wirds', ['entries' => [$entry]])
            ->assertOk()->assertJsonPath('entries.0.deleted', false);
    }

    public function test_users_only_see_their_own_entries(): void
    {
        $alice = User::factory()->create();
        $bob = User::factory()->create();
        $shared = $this->entry();

        Sanctum::actingAs($alice);
        $this->postJson('/api/quran/wirds', ['entries' => [$shared]])->assertOk();

        Sanctum::actingAs($bob);
        $this->getJson('/api/quran/wirds')->assertOk()->assertJsonCount(0, 'entries');
        // Same client_id from another user is a separate record, not a takeover.
        $this->postJson('/api/quran/wirds', ['entries' => [array_merge($shared, ['from_ayah_id' => 1, 'to_ayah_id' => 7])]])
            ->assertOk();
        $this->assertSame(13, WirdEntry::where('user_id', $alice->id)->first()->ayahCount());
    }

    public function test_stats_today_week_streak_and_next_ayah(): void
    {
        $this->travelTo(CarbonImmutable::parse('2026-09-21 12:00', 'Asia/Riyadh'));
        Sanctum::actingAs(User::factory()->create());

        $at = fn (string $local) => CarbonImmutable::parse($local, 'Asia/Riyadh')->toIso8601String();
        $this->postJson('/api/quran/wirds', ['entries' => [
            $this->entry(['from_ayah_id' => 8, 'to_ayah_id' => 20, 'read_at' => $at('2026-09-21 06:00')]),   // today: 13
            $this->entry(['from_ayah_id' => 21, 'to_ayah_id' => 30, 'read_at' => $at('2026-09-20 23:30')]),  // yesterday 23:30 local (20:30 UTC): 10 — must not count as today
            $this->entry(['from_ayah_id' => 31, 'to_ayah_id' => 35, 'read_at' => $at('2026-09-19 22:00')]),  // 2 days ago: 5
            $this->entry(['from_ayah_id' => 1, 'to_ayah_id' => 7, 'read_at' => $at('2026-09-10 22:00')]),    // outside week
        ]])->assertOk();

        $this->getJson('/api/quran/wirds/stats?tz=Asia/Riyadh')
            ->assertOk()
            ->assertJson([
                'today_ayahs' => 13,
                'week_ayahs' => 28,
                'streak_days' => 3,
                'next_ayah_id' => 21, // after the most recent wird (8–20)
            ]);
    }
}
