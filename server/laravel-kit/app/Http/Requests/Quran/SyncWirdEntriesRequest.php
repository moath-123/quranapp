<?php

namespace App\Http\Requests\Quran;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Validator;

class SyncWirdEntriesRequest extends FormRequest
{
    public const MAX_AYAH_ID = 6236;

    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    public function rules(): array
    {
        return [
            'entries' => ['required', 'array', 'min:1', 'max:200'],
            'entries.*.client_id' => ['required', 'uuid', 'distinct'],
            'entries.*.from_ayah_id' => ['required', 'integer', 'between:1,'.self::MAX_AYAH_ID],
            'entries.*.to_ayah_id' => ['required', 'integer', 'between:1,'.self::MAX_AYAH_ID],
            'entries.*.read_at' => ['required', 'date', 'before_or_equal:'.now()->addMinutes(5)->toIso8601String()],
            'entries.*.deleted' => ['sometimes', 'boolean'],
        ];
    }

    public function after(): array
    {
        return [
            function (Validator $validator) {
                foreach ((array) $this->input('entries', []) as $i => $entry) {
                    if (isset($entry['from_ayah_id'], $entry['to_ayah_id'])
                        && (int) $entry['to_ayah_id'] < (int) $entry['from_ayah_id']) {
                        $validator->errors()->add("entries.$i.to_ayah_id", 'to_ayah_id must be >= from_ayah_id.');
                    }
                }
            },
        ];
    }
}
