/**
 * Динамическая часть конфига поверх `app.json`.
 *
 * Expo читает `app.json`, нормализует и передаёт результат сюда как `config`
 * (docs.expo.dev/workflow/configuration). Всё постоянное живёт в `app.json`,
 * здесь — только то, что зависит от того, какую сборку делаем.
 *
 * <h2>Зачем понадобилось: http нужен ровно одной сборке из трёх</h2>
 * Боевой домен с 30.08.2026 работает по `https`, и релизным сборкам открытый
 * http не нужен вовсе. А вот dev-сборка ходит на стенд разработчика —
 * `http://<IP>:8099` в локальной сети, где сертификата нет и не будет.
 *
 * Android с 9-й версии режет открытый http, iOS — через ATS. Раньше
 * послабление стояло в `app.json`, то есть попадало и в APK для заказчика:
 * приложение молча соглашалось ходить куда угодно по открытому каналу.
 * Теперь его включает только профиль `development` (см. `eas.json`).
 *
 * ⚠️ Проверять надо ОБЕ ветки, значение читается на этапе сборки:
 *
 *   npx expo config --type prebuild | grep -i cleartext                  # пусто
 *   ALLOW_CLEARTEXT=true npx expo config --type prebuild | grep -i cleartext
 */
const ALLOW_CLEARTEXT = process.env.ALLOW_CLEARTEXT === 'true';

module.exports = ({ config }) => {
  if (!ALLOW_CLEARTEXT) {
    return config;
  }

  return {
    ...config,
    ios: {
      ...config.ios,
      infoPlist: {
        ...config.ios?.infoPlist,
        // Именно локальная сеть, а не «разрешить всё»: точечное послабление
        // для стенда по LAN, боевой домен под него не подпадает.
        NSAppTransportSecurity: { NSAllowsLocalNetworking: true },
      },
    },
    plugins: [
      ...(config.plugins ?? []),
      ['expo-build-properties', { android: { usesCleartextTraffic: true } }],
    ],
  };
};
