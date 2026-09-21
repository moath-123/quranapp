<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Ayahs are identified by their mushaf position 1..6236, never by page number,
        // so saved data stays valid whatever mushaf edition is displayed.
        Schema::create('wird_entries', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->uuid('client_id');
            $table->unsignedSmallInteger('from_ayah_id');
            $table->unsignedSmallInteger('to_ayah_id');
            $table->timestamp('read_at');
            $table->timestamps();
            $table->softDeletes();
            $table->unique(['user_id', 'client_id']);
            $table->index(['user_id', 'read_at']);
            $table->index(['user_id', 'updated_at']);
        });

        Schema::create('ayah_bookmarks', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->unsignedSmallInteger('ayah_id');
            $table->timestamps();
            $table->unique(['user_id', 'ayah_id']);
        });

        Schema::create('reading_positions', function (Blueprint $table) {
            $table->foreignId('user_id')->primary()->constrained()->cascadeOnDelete();
            $table->unsignedSmallInteger('ayah_id');
            $table->unsignedSmallInteger('page');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('reading_positions');
        Schema::dropIfExists('ayah_bookmarks');
        Schema::dropIfExists('wird_entries');
    }
};
