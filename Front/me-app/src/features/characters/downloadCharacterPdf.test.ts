
import { downloadCharacterPdf } from './downloadCharacterPdf';

describe('downloadCharacterPdf', () => {
  const blob = new Blob(['%PDF-1.4 stub'], { type: 'application/pdf' });
  const realCreateObjectURL = URL.createObjectURL;
  const realRevokeObjectURL = URL.revokeObjectURL;

  beforeEach(() => {

    URL.createObjectURL = jest.fn(() => 'blob:fake-url');
    URL.revokeObjectURL = jest.fn();
    document.body.innerHTML = '';
    jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
  });

  afterEach(() => {
    URL.createObjectURL = realCreateObjectURL;
    URL.revokeObjectURL = realRevokeObjectURL;
    jest.restoreAllMocks();
  });

  test('UT-F-10: подставляет Authorization header при наличии токена', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      blob: () => Promise.resolve(blob),
    });
    global.fetch = fetchMock as unknown as typeof fetch;

    await downloadCharacterPdf(42, 'Аркан', 'tok123');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/characters/42/pdf');
    expect((init as RequestInit).headers).toMatchObject({
      Authorization: 'Bearer tok123',
    });
  });

  test('UT-F-11: без токена Authorization не выставляется', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      blob: () => Promise.resolve(blob),
    });
    global.fetch = fetchMock as unknown as typeof fetch;

    await downloadCharacterPdf(1, 'X', null);

    const [, init] = fetchMock.mock.calls[0];
    expect((init as RequestInit).headers).toEqual({});
  });

  test('UT-F-12: имя файла безопасно очищается от запрещённых символов', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      blob: () => Promise.resolve(blob),
    });
    global.fetch = fetchMock as unknown as typeof fetch;


    const createElementSpy = jest.spyOn(document, 'createElement');

    await downloadCharacterPdf(7, 'A/B<C>D:E*F|G', 'tok');

    const anchors = createElementSpy.mock.results
      .filter((r) => (r.value as HTMLElement).tagName === 'A')
      .map((r) => r.value as HTMLAnchorElement);

    expect(anchors.length).toBeGreaterThan(0);
    const a = anchors[anchors.length - 1];
    expect(a.download).toMatch(/\.pdf$/);
    expect(a.download).not.toMatch(/[<>:"\/\\|?*]/);
  });

  test('UT-F-13: при !ok пробрасывает ошибку с текстом', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: false,
      status: 403,
      text: () => Promise.resolve('Forbidden!'),
    });
    global.fetch = fetchMock as unknown as typeof fetch;

    await expect(downloadCharacterPdf(99, 'p', null))
      .rejects.toThrow(/Forbidden!/);
  });
});
