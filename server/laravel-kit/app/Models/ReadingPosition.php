<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ReadingPosition extends Model
{
    protected $primaryKey = 'user_id';

    public $incrementing = false;

    protected $fillable = ['user_id', 'ayah_id', 'page'];

    protected function casts(): array
    {
        return ['ayah_id' => 'integer', 'page' => 'integer'];
    }
}
