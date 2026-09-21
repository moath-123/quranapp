<?php

namespace Tests\Feature\Quran;

use App\Models\User;
use App\Support\Quran\Rollout;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class AppConfigTest extends TestCase
{
    use RefreshDatabase;

    private function flag(array $headers = []): bool
    {
        return $this->getJson('/api/quran/app-config', $headers)->assertOk()->json('flags.new_mushaf');
    }

    public function test_off_by_default(): void
    {
        config(['quran.new_mushaf' => ['enabled' => false, 'rollout' => 100, 'allowlist' => []]]);
        Sanctum::actingAs(User::factory()->create());

        $this->assertFalse($this->flag());
    }

    public function test_kill_switch_beats_rollout_and_allowlist(): void
    {
        $user = User::factory()->create();
        config(['quran.new_mushaf' => ['enabled' => false, 'rollout' => 100, 'allowlist' => [$user->id]]]);
        Sanctum::actingAs($user);

        $this->assertFalse($this->flag());
    }

    public function test_full_rollout_turns_it_on_for_users_and_devices(): void
    {
        config(['quran.new_mushaf' => ['enabled' => true, 'rollout' => 100, 'allowlist' => []]]);

        $this->assertTrue($this->flag(['X-Device-Id' => 'device-123']));
        Sanctum::actingAs(User::factory()->create());
        $this->assertTrue($this->flag());
    }

    public function test_zero_rollout_keeps_everyone_off_except_allowlist(): void
    {
        $tester = User::factory()->create();
        config(['quran.new_mushaf' => ['enabled' => true, 'rollout' => 0, 'allowlist' => [$tester->id]]]);

        Sanctum::actingAs(User::factory()->create());
        $this->assertFalse($this->flag());

        Sanctum::actingAs($tester);
        $this->assertTrue($this->flag());
    }

    public function test_guest_without_device_id_stays_off(): void
    {
        config(['quran.new_mushaf' => ['enabled' => true, 'rollout' => 100, 'allowlist' => []]]);

        $this->assertFalse($this->flag());
    }

    public function test_rollout_is_stable_and_monotonic(): void
    {
        config(['quran.new_mushaf' => ['enabled' => true, 'rollout' => 5, 'allowlist' => []]]);
        $at5 = collect(range(1, 2000))->filter(fn ($id) => Rollout::newMushafFor("user:$id"))->values();

        config(['quran.new_mushaf.rollout' => 25]);
        $at25 = collect(range(1, 2000))->filter(fn ($id) => Rollout::newMushafFor("user:$id"))->values();

        // Same answer every time, everyone from 5% is still in at 25%, and the shares are roughly right.
        $this->assertSame($at25->all(), collect(range(1, 2000))->filter(fn ($id) => Rollout::newMushafFor("user:$id"))->values()->all());
        $this->assertEmpty($at5->diff($at25));
        $this->assertEqualsWithDelta(100, $at5->count(), 40);
        $this->assertEqualsWithDelta(500, $at25->count(), 80);
    }

    public function test_pages_settings_come_from_config(): void
    {
        config(['quran.pages' => ['url' => null, 'sha256' => null, 'version' => 1]]);
        $this->getJson('/api/quran/app-config')->assertOk()->assertJsonPath('pages', null);

        config(['quran.pages' => [
            'url' => 'https://cdn.taahud.example/quran/images_1024-v1.zip',
            'sha256' => '882e5c7bc8d43a4efac679e7c85b64e1fc059783dab6babc02365f2c36a286cb',
            'version' => 1,
        ]]);
        $this->getJson('/api/quran/app-config')
            ->assertOk()
            ->assertJsonPath('pages.url', 'https://cdn.taahud.example/quran/images_1024-v1.zip')
            ->assertJsonPath('pages.version', 1)
            ->assertJsonPath('pages.sha256', '882e5c7bc8d43a4efac679e7c85b64e1fc059783dab6babc02365f2c36a286cb');
    }
}
