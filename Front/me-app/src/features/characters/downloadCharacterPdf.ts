/**
 * Downloads a character sheet as a PDF.
 *
 * The function calls `GET /api/v1/characters/{id}/pdf` with a Bearer token,
 * reads the body as a `Blob` and triggers the browser download via a
 * temporary `<a download>` element. The proposed filename is the character
 * name sanitized to letters/digits/spaces/dashes (Unicode-aware); when the
 * sanitized name is empty `character-<id>` is used as a fallback.
 *
 * @param characterId   character identifier
 * @param characterName original character name (used as filename basis)
 * @param accessToken   user's access token, or `null` to skip the
 *                      Authorization header (anonymous public characters)
 * @throws Error if the server responds with a non-2xx status; the body of
 *               the response is forwarded as the error message when present
 * @module
 */
export async function downloadCharacterPdf(
  characterId: number,
  characterName: string,
  accessToken: string | null,
): Promise<void> {
  const headers: Record<string, string> = {};
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }
  const resp = await fetch(`/api/v1/characters/${characterId}/pdf`, {
    method: 'GET',
    headers,
    credentials: 'same-origin',
  });
  if (!resp.ok) {
    const msg = await resp.text();
    throw new Error(msg || `Server returned status ${resp.status}`);
  }
  const blob = await resp.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  const safeName = characterName.replace(/[^\p{L}\p{N}\s\-_]/gu, '').trim()
    || `character-${characterId}`;
  a.download = `${safeName}.pdf`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
