const { isAuthorized } = require('../src/telegram/adminBot');

describe('Telegram Admin Bot Security Checks', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  test('Denies access when no admin variables set', () => {
    delete process.env.ADMIN_TELEGRAM_CHAT_ID;
    delete process.env.ADMIN_TELEGRAM_USER_IDS;

    const msg = { from: { id: 12345 }, chat: { id: 12345 } };
    expect(isAuthorized(msg)).toBe(false);
  });

  test('Allows access when ADMIN_TELEGRAM_CHAT_ID matches', () => {
    process.env.ADMIN_TELEGRAM_CHAT_ID = '999888';

    const validMsg = { from: { id: 123 }, chat: { id: 999888 } };
    expect(isAuthorized(validMsg)).toBe(true);

    const invalidMsg = { from: { id: 123 }, chat: { id: 111111 } };
    expect(isAuthorized(invalidMsg)).toBe(false);
  });

  test('Allows access when user ID is in ADMIN_TELEGRAM_USER_IDS list', () => {
    process.env.ADMIN_TELEGRAM_USER_IDS = '1001, 1002, 1003';

    const validMsg = { from: { id: 1002 }, chat: { id: 55555 } };
    expect(isAuthorized(validMsg)).toBe(true);

    const invalidMsg = { from: { id: 9999 }, chat: { id: 55555 } };
    expect(isAuthorized(invalidMsg)).toBe(false);
  });
});
