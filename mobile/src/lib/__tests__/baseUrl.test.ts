import { isLocalApi } from '../api';

/**
 * Локальный стенд отличается от боевого домена ПО АДРЕСУ.
 *
 * <h2>Почему не по флагу сборки</h2>
 * Соблазн написать `__DEV__` велик, но dev-клиент точно так же подключают
 * к боевому домену (чтобы посмотреть настоящие данные), а release-сборку —
 * к стенду перед выкаткой. Флаг сборки отвечает на другой вопрос.
 *
 * <h2>Что от этого зависит</h2>
 * Метка в адресе картинок. На стенде база H2 живёт в памяти и после
 * рестарта раздаёт ТЕ ЖЕ id другим файлам — `expo-image` кэширует по URL и
 * показывает вчерашнюю заглушку. На боевом домене id постоянны, и метка
 * там только ломала бы кэш: каждый запуск заново качал бы все афиши.
 *
 * Ошибка здесь тихая в обе стороны, поэтому проверяется списком.
 */
describe('isLocalApi', () => {
  it('локальные адреса', () => {
    expect(isLocalApi('http://localhost:8081')).toBe(true);
    expect(isLocalApi('http://127.0.0.1:8099')).toBe(true);
    expect(isLocalApi('http://192.168.1.103:8099')).toBe(true);
    expect(isLocalApi('http://10.0.2.2:8099')).toBe(true);
    expect(isLocalApi('http://macbook.local:8099')).toBe(true);
  });

  it('боевой домен локальным не считается', () => {
    expect(isLocalApi('http://uzcasting.com')).toBe(false);
    expect(isLocalApi('https://uzcasting.com')).toBe(false);
    expect(isLocalApi('https://uzcasting.site')).toBe(false);
  });

  /**
   * ⚠️ Частная сеть — это 172.16–172.31, а не весь диапазон `172.*`.
   * `172.32.x` и `172.8.x` — обычные публичные адреса, и «оптимизация»
   * до `startsWith('172.')` молча ломала бы кэш на реальном сервере.
   */
  it('из диапазона 172 частная только середина', () => {
    expect(isLocalApi('http://172.16.0.1:8099')).toBe(true);
    expect(isLocalApi('http://172.31.255.255:8099')).toBe(true);
    expect(isLocalApi('http://172.15.0.1')).toBe(false);
    expect(isLocalApi('http://172.32.0.1')).toBe(false);
  });

  /**
   * Похожее начало не делает домен локальным: `10.example.com` и
   * `192.168.example.com` — обычные хосты в интернете.
   */
  it('домен, начинающийся с цифр, не путается с сетью', () => {
    expect(isLocalApi('https://10.example.com')).toBe(false);
    expect(isLocalApi('https://192.168.example.com')).toBe(false);
  });
});
