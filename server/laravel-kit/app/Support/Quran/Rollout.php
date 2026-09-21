<?php

namespace App\Support\Quran;

/**
 * Decides whether a user gets the new mushaf. Buckets are stable (same user → same bucket),
 * so raising the rollout from 5% to 25% keeps everyone who already had it.
 */
class Rollout
{
    public static function bucket(string $subject): int
    {
        return crc32('new_mushaf:'.$subject) % 100;
    }

    /**
     * @param  string|null  $subject  user id, or a device id for guests; null → off
     */
    public static function newMushafFor(?string $subject, ?int $userId = null): bool
    {
        $config = config('quran.new_mushaf');

        if (! $config['enabled']) {
            return false;
        }
        if ($userId !== null && in_array($userId, $config['allowlist'], true)) {
            return true;
        }
        if ($subject === null || $subject === '') {
            return false;
        }

        $rollout = max(0, min(100, (int) $config['rollout']));

        return self::bucket($subject) < $rollout;
    }
}
