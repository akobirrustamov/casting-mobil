import { Ionicons } from '@expo/vector-icons';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, View, useWindowDimensions } from 'react-native';

import { LanguageSwitcher } from '@/components/ui/LanguageSwitcher';
import { TOUCH_TARGET, colors } from '@/theme/tokens';

/**
 * Шестерёнка на экранах входа — выбор языка.
 *
 * <h2>Зачем она здесь</h2>
 * Заказчик (09.09.2026) прислал экран входа с круглой шестерёнкой справа
 * от знака и подтвердил: «shesternya til tanlash uchun». До этого язык
 * переключался ТОЛЬКО в профиле — то есть уже после входа. Человек,
 * которому узбекский интерфейс непонятен, до профиля не доходил: он
 * упирался в первый же экран, где всё написано на языке, который он и
 * хотел сменить.
 *
 * <h2>Почему всплывашка, а не строка в потоке</h2>
 * В профиле переключатель раздвигает список — там это нормально. Здесь
 * так нельзя: весь каркас входа (`AuthScaffold`) держится на том, что
 * поле стоит на одной высоте на всех трёх шагах и не шевелится. Строка,
 * появляющаяся под знаком, сдвинула бы форму ровно в тот момент, когда
 * человек целится в поле.
 *
 * Поэтому выбор открывается в `Modal` — он вообще не участвует в
 * раскладке экрана. Заодно появляется то, чего у всплывашки внутри
 * прокрутки не было бы: затемнение и закрытие касанием мимо.
 *
 * <h2>Почему координаты меряются, а не задаются</h2>
 * Кнопка живёт ВНУТРИ прокрутки (уезжает вместе со знаком, как просил
 * заказчик 03.09), а карточка выбора — в `Modal`, то есть в координатах
 * окна. Связать их можно только замером: `measureInWindow` на нажатии.
 * Посчитать «на глаз» нельзя — знак меняет размер от экрана к экрану
 * (см. `markSizeFor`), и вместе с ним едет вся колонка.
 */

/** Ширина карточки выбора: три сегмента с подписями «O'zbekcha» и «Русский». */
const MENU_WIDTH = 264;

/** Зазор между кнопкой и карточкой. */
const MENU_GAP = 8;

/** Отступ карточки от края экрана, если кнопка стоит у самого края. */
const SCREEN_PADDING = 12;

/** Куда открыться, если замер не состоялся (см. `open`). */
const FALLBACK_MENU_TOP = 96;

export function AuthLanguageButton({
  /**
   * Сдвиг кнопки от верха блока со знаком.
   *
   * Приходит снаружи: по референсу шестерёнка стоит примерно на нижней
   * кромке знака, а знак меняет размер (см. `markSizeFor`) — привязать
   * её постоянным числом значило бы попасть на референсе и промахнуться
   * на всех остальных экранах.
   */
  top,
}: {
  top: number;
}) {
  const { t } = useTranslation();
  const { width } = useWindowDimensions();
  const button = useRef<View>(null);
  const [menu, setMenu] = useState<{ top: number; right: number } | null>(null);

  const open = () => {
    const node = button.current;

    // ⚠️ Карточка открывается СРАЗУ, ещё до замера.
    //
    // Раньше запасные координаты ставились только когда метода
    // `measureInWindow` нет вовсе. Но метод может БЫТЬ и при этом
    // никогда не вызвать колбэк: так бывает, если узел ещё не разложен
    // (нажали во время перехода между экранами) или уже снят. Тогда
    // `setMenu` не выполнялся вообще, и кнопка просто не отвечала на
    // нажатие — ровно та «мёртвая кнопка», которой мы и хотели
    // избежать.
    //
    // Теперь порядок обратный: сначала открыть, потом уточнить место.
    // Худший случай — карточка долю секунды стоит не под кнопкой; это
    // заметно меньшая беда, чем экран, который не отвечает.
    setMenu({ top: FALLBACK_MENU_TOP, right: SCREEN_PADDING });

    if (typeof node?.measureInWindow !== 'function') {
      return;
    }

    node.measureInWindow((x, y, w, h) => {
      setMenu({
        top: y + h + MENU_GAP,
        // Правым краем карточка равняется по правому краю кнопки, но не
        // упирается в край экрана: на узком телефоне она иначе вылезала бы.
        right: Math.max(SCREEN_PADDING, width - (x + w)),
      });
    });
  };

  return (
    <>
      <Pressable
        ref={button}
        onPress={open}
        accessibilityRole="button"
        accessibilityLabel={t('profile.language')}
        style={{
          position: 'absolute',
          right: 20,
          top,
          width: TOUCH_TARGET,
          height: TOUCH_TARGET,
          borderRadius: TOUCH_TARGET / 2,
          alignItems: 'center',
          justifyContent: 'center',
          // Кружок с референса: чуть светлее фона, с тонкой кромкой.
          // Сплошная `surface` на чёрном не читалась бы вовсе.
          backgroundColor: 'rgba(255,255,255,0.07)',
          borderWidth: 1,
          borderColor: colors.border,
        }}
        className="active:opacity-70"
      >
        <Ionicons name="settings-sharp" size={20} color={colors.textMuted} />
      </Pressable>

      <Modal
        visible={menu !== null}
        transparent
        animationType="fade"
        onRequestClose={() => setMenu(null)}
        statusBarTranslucent
      >
        {/* Затемнение и оно же — «закрыть касанием мимо». */}
        <Pressable
          onPress={() => setMenu(null)}
          style={{ flex: 1, backgroundColor: 'rgba(0,0,0,0.55)' }}
        >
          {menu ? (
            // ⚠️ Внешний `Pressable` без обработчика: он гасит касание по
            // самой карточке, иначе выбор языка закрывал бы её ещё до
            // того, как палец дойдёт до сегмента.
            <Pressable
              onPress={() => {}}
              style={{
                position: 'absolute',
                top: menu.top,
                right: menu.right,
                width: MENU_WIDTH,
              }}
            >
              <View className="rounded-card-lg border border-border bg-surface-2 p-1.5">
                <LanguageSwitcher onSelect={() => setMenu(null)} />
              </View>
            </Pressable>
          ) : null}
        </Pressable>
      </Modal>
    </>
  );
}
