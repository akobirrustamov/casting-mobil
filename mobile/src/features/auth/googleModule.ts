import { GOOGLE_CLIENT_IDS } from './config';

/**
 * Нативный модуль Google — загрузка, которая НЕ роняет экран входа.
 *
 * <h2>⚠️ Что здесь чинится</h2>
 * `@react-native-google-signin/google-signin` — нативный модуль, и он
 * привязывается к нативному коду ПРЯМО НА ИМПОРТЕ:
 *
 *     TurboModuleRegistry.getEnforcing('RNGoogleSignin')   // spec/NativeGoogleSignin
 *     NativeModule.getConstants()                          // errors/errorCodes
 *
 * `getEnforcing` — это именно «enforcing»: если нативной части в сборке
 * нет, он БРОСАЕТ на этапе вычисления модуля. То есть один только
 * `import ... from '@react-native-google-signin/google-signin'` в
 * `useGoogleSignIn.ts` валил ВЕСЬ экран входа — вместе с номером,
 * паролем и регистрацией, к которым Google не имеет отношения.
 *
 * Проверки в `config.ts` (`isExpoGo`, есть ли client ID) от этого не
 * спасали: они выполняются ПОСЛЕ импорта, а падало на импорте.
 *
 * Так происходит в двух совершенно обычных случаях:
 * <ul>
 *   <li>Expo Go — нативных модулей туда добавить нельзя в принципе;</li>
 *   <li>dev-сборка, собранная ДО появления этой зависимости, — Metro
 *       отдаёт новый JS по воздуху, а нативной части в APK нет.</li>
 * </ul>
 *
 * <h2>Почему `require`, а не `import`</h2>
 * `import` поднимается наверх и выполняется безусловно — обернуть его в
 * `try` нельзя. `require` вычисляется в момент вызова, поэтому здесь
 * ошибка ловится и превращается в «кнопка Google неактивна», а не в
 * красный экран.
 *
 * Результат кэшируется: повторный `require` упавшего модуля Metro
 * выполняет заново, а нам достаточно узнать это один раз.
 */
type GoogleModule = typeof import('@react-native-google-signin/google-signin');

/** `undefined` — ещё не пробовали, `null` — нативной части нет. */
let cached: GoogleModule | null | undefined;

function load(): GoogleModule | null {
  if (cached === undefined) {
    try {
      // eslint-disable-next-line @typescript-eslint/no-require-imports
      cached = require('@react-native-google-signin/google-signin') as GoogleModule;
    } catch {
      cached = null;
    }
  }
  return cached;
}

/** Есть ли нативная часть в этой сборке. */
export function isGoogleNativeAvailable(): boolean {
  return load() !== null;
}

let configured = false;

/**
 * Готовый к работе `GoogleSignin` либо `null`, если модуля нет.
 *
 * ⚠️ `configure` вызывается здесь, а не на импорте: раньше он стоял в
 * теле модуля и падал ровно там же, где и всё остальное.
 *
 * В `configure` уходит ТОЛЬКО веб-клиент: именно его id окажется в `aud`
 * выданного токена, и по нему бэкенд токен проверяет. Android-клиент в
 * коде не упоминается — Google узнаёт приложение по паре «package +
 * SHA-1» из консоли.
 */
export function googleSignin(): GoogleModule['GoogleSignin'] | null {
  const mod = load();
  if (!mod) return null;

  if (!configured) {
    mod.GoogleSignin.configure({
      webClientId: GOOGLE_CLIENT_IDS.web,
      iosClientId: GOOGLE_CLIENT_IDS.ios,
    });
    configured = true;
  }
  return mod.GoogleSignin;
}

/**
 * Коды ошибок нативного модуля.
 *
 * `null`, если модуля нет: тогда и ошибок от него взяться неоткуда, а
 * сравнивать с выдуманными строками — молча путать отмену с поломкой.
 */
export function googleStatusCodes(): GoogleModule['statusCodes'] | null {
  return load()?.statusCodes ?? null;
}
