<?php

use App\Http\Controllers\Quran\AppConfigController;
use App\Http\Controllers\Quran\BookmarkController;
use App\Http\Controllers\Quran\ReadingPositionController;
use App\Http\Controllers\Quran\WirdController;
use Illuminate\Support\Facades\Route;

// Included from routes/api.php (prefix "api"): require __DIR__.'/quran.php';

Route::prefix('quran')->group(function () {
    // Public: guests are bucketed by X-Device-Id, signed-in users by user id.
    Route::get('app-config', AppConfigController::class)->middleware('throttle:60,1');

    Route::middleware(['auth:sanctum', 'throttle:120,1'])->group(function () {
        Route::get('wirds', [WirdController::class, 'index']);
        Route::post('wirds', [WirdController::class, 'sync']);
        Route::get('wirds/stats', [WirdController::class, 'stats']);

        Route::get('bookmarks', [BookmarkController::class, 'index']);
        Route::put('bookmarks', [BookmarkController::class, 'update']);

        Route::get('position', [ReadingPositionController::class, 'show']);
        Route::put('position', [ReadingPositionController::class, 'update']);
    });
});
