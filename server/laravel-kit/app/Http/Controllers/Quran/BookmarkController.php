<?php

namespace App\Http\Controllers\Quran;

use App\Http\Controllers\Controller;
use App\Models\AyahBookmark;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class BookmarkController extends Controller
{
    /** GET /api/quran/bookmarks */
    public function index(Request $request): JsonResponse
    {
        return response()->json([
            'ayah_ids' => AyahBookmark::where('user_id', $request->user()->id)->orderBy('ayah_id')->pluck('ayah_id'),
        ]);
    }

    /** PUT /api/quran/bookmarks {add: [ids], remove: [ids]} — idempotent. */
    public function update(Request $request): JsonResponse
    {
        $data = $request->validate([
            'add' => ['sometimes', 'array', 'max:500'],
            'add.*' => ['integer', 'between:1,6236'],
            'remove' => ['sometimes', 'array', 'max:500'],
            'remove.*' => ['integer', 'between:1,6236'],
        ]);
        $userId = $request->user()->id;

        DB::transaction(function () use ($data, $userId) {
            foreach (array_unique($data['add'] ?? []) as $ayahId) {
                AyahBookmark::firstOrCreate(['user_id' => $userId, 'ayah_id' => $ayahId]);
            }
            if (! empty($data['remove'])) {
                AyahBookmark::where('user_id', $userId)->whereIn('ayah_id', $data['remove'])->delete();
            }
        });

        return $this->index($request);
    }
}
