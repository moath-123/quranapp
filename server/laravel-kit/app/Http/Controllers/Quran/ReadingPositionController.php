<?php

namespace App\Http\Controllers\Quran;

use App\Http\Controllers\Controller;
use App\Models\ReadingPosition;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ReadingPositionController extends Controller
{
    /** GET /api/quran/position */
    public function show(Request $request): JsonResponse
    {
        $position = ReadingPosition::find($request->user()->id);

        return response()->json($position ? [
            'ayah_id' => $position->ayah_id,
            'page' => $position->page,
            'updated_at' => $position->updated_at->toIso8601String(),
        ] : null);
    }

    /** PUT /api/quran/position {ayah_id, page} */
    public function update(Request $request): JsonResponse
    {
        $data = $request->validate([
            'ayah_id' => ['required', 'integer', 'between:1,6236'],
            'page' => ['required', 'integer', 'between:1,604'],
        ]);

        ReadingPosition::updateOrCreate(['user_id' => $request->user()->id], $data);

        return $this->show($request);
    }
}
