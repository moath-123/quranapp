<?php

namespace Tests\Feature\Quran;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class BookmarksAndPositionTest extends TestCase
{
    use RefreshDatabase;

    public function test_bookmarks_add_and_remove_idempotently(): void
    {
        Sanctum::actingAs(User::factory()->create());

        $this->getJson('/api/quran/bookmarks')->assertOk()->assertJsonPath('ayah_ids', []);
        $this->putJson('/api/quran/bookmarks', ['add' => [79, 1, 79]])
            ->assertOk()->assertJsonPath('ayah_ids', [1, 79]);
        $this->putJson('/api/quran/bookmarks', ['add' => [1], 'remove' => [79, 500]])
            ->assertOk()->assertJsonPath('ayah_ids', [1]);
        $this->putJson('/api/quran/bookmarks', ['add' => [0]])
            ->assertUnprocessable();
    }

    public function test_reading_position(): void
    {
        Sanctum::actingAs(User::factory()->create());

        $this->getJson('/api/quran/position')->assertOk()->assertExactJson([]);
        $this->putJson('/api/quran/position', ['ayah_id' => 746, 'page' => 121])
            ->assertOk()->assertJsonPath('ayah_id', 746)->assertJsonPath('page', 121);
        $this->putJson('/api/quran/position', ['ayah_id' => 747, 'page' => 121])
            ->assertOk()->assertJsonPath('ayah_id', 747);
        $this->putJson('/api/quran/position', ['ayah_id' => 1, 'page' => 605])->assertUnprocessable();
    }
}
