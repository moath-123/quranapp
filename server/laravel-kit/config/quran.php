<?php

return [

    /*
    | new_mushaf — gradual rollout of the new mushaf (docs/pilot-checklist.md).
    |   enabled:   global kill switch. false = everyone stays on the current mushaf, immediately.
    |   rollout:   0–100, share of users who get it (stable per user: same answer on every request).
    |   allowlist: user ids that always get it (internal testers), even at rollout 0.
    */
    'new_mushaf' => [
        'enabled' => (bool) env('QURAN_NEW_MUSHAF_ENABLED', false),
        'rollout' => (int) env('QURAN_NEW_MUSHAF_ROLLOUT', 0),
        'allowlist' => array_values(array_filter(array_map(
            'intval',
            explode(',', (string) env('QURAN_NEW_MUSHAF_ALLOWLIST', ''))
        ))),
    ],

    /*
    | Page images (tools/package_pages.sh → dist/pages/pages-manifest.json).
    | Leave url empty to let the app use its default (Quran.com).
    */
    'pages' => [
        'url' => env('QURAN_PAGES_URL'),
        'sha256' => env('QURAN_PAGES_SHA256'),
        'version' => (int) env('QURAN_PAGES_VERSION', 1),
    ],

];
