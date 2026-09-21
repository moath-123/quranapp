<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\SoftDeletes;

class WirdEntry extends Model
{
    use SoftDeletes;

    protected $fillable = ['user_id', 'client_id', 'from_ayah_id', 'to_ayah_id', 'read_at'];

    protected function casts(): array
    {
        return [
            'from_ayah_id' => 'integer',
            'to_ayah_id' => 'integer',
            'read_at' => 'datetime',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function ayahCount(): int
    {
        return $this->to_ayah_id - $this->from_ayah_id + 1;
    }

    public function toSyncArray(): array
    {
        return [
            'client_id' => $this->client_id,
            'from_ayah_id' => $this->from_ayah_id,
            'to_ayah_id' => $this->to_ayah_id,
            'ayah_count' => $this->ayahCount(),
            'read_at' => $this->read_at->toIso8601String(),
            'deleted' => $this->trashed(),
            'updated_at' => $this->updated_at->toIso8601String(),
        ];
    }
}
