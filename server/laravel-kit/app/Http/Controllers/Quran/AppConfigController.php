<?php

namespace App\Http\Controllers\Quran;

use App\Http\Controllers\Controller;
use App\Support\Quran\Rollout;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * GET /api/quran/app-config — flags + page-image settings for the app.
 * Works for signed-in users (bucketed by user id) and guests (bucketed by the X-Device-Id header).
 */
class AppConfigController extends Controller
{
    public function __invoke(Request $request): JsonResponse
    {
        $user = $request->user('sanctum');
        $subject = $user ? 'user:'.$user->getAuthIdentifier() : $this->deviceSubject($request);

        $pages = config('quran.pages');

        return response()->json([
            'flags' => [
                'new_mushaf' => Rollout::newMushafFor($subject, $user?->getAuthIdentifier()),
            ],
            'pages' => $pages['url'] ? [
                'url' => $pages['url'],
                'sha256' => $pages['sha256'] ?: null,
                'version' => $pages['version'],
            ] : null,
        ])->header('Cache-Control', 'private, max-age=300');
    }

    private function deviceSubject(Request $request): ?string
    {
        $device = trim((string) $request->header('X-Device-Id'));

        return $device !== '' && strlen($device) <= 128 ? 'device:'.$device : null;
    }
}
