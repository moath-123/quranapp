<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class AyahBookmark extends Model
{
    protected $fillable = ['user_id', 'ayah_id'];

    protected function casts(): array
    {
        return ['ayah_id' => 'integer'];
    }
}
