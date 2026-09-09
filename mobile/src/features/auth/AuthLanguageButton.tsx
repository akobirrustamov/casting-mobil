import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal, Pressable, View } from 'react-native';

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
 * <h2>Откуда карточка знает, где стоять</h2>
 * Кнопка живёт ВНУТРИ прокрутки, а карточка — в `Modal`, то есть в
 * координатах окна. Место считается, а не меряется: вызывающий знает
 * оба слагаемых — где начинается блок со знаком (`windowTop`) и на
 * сколько кнопка опущена внутри него (`top`).
 *
 * ⚠️ Напрашивающийся `measureInWindow` тут ХУЖЕ, а не точнее. Он
 * добавляет ветку, которая в тестовом рендерере не выполняется никогда
 * (метод есть, обработчик не зовёт) — то есть открытие карточки,
 * единственное поведение этой кнопки, осталось бы непроверенным.
 *
 * Расчёт расходится с замером ровно на прокрутку, а экраны входа
 * специально собраны так, чтобы не прокручиваться (см. `markSizeFor`).
 * На самых низких телефонах запас всё же уезжает — там карточка встанет
 * на те же ~30 пунктов выше кнопки, оставаясь рядом с ней.
 */

/** Ширина карточки выбора: три сегмента с подписями «O'zbekcha» и «Русский». */
const MENU_WIDTH = 264;

/** Зазор между кнопкой и карточкой. */
const MENU_GAP = 8;

/** Отступ кнопки от правого края экрана. Карточка равняется по нему же. */
const BUTTON_RIGHT = 20;

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
  /** Где верх блока со знаком в координатах ОКНА. */
  windowTop,
}: {
  top: number;
  windowTop: number;
}) {
  const { t } = useTranslation();
  const { width } = useWindowDimensions();
  const button = useRef<View>(null);
  const [menu, setMenu] = useState<{ top: number; right: number } | null>(null);

  const open = () => {
    const node = button.current;

    /**
     * ⚠️ Карточка открывается СРАЗУ, ещё до замера, и только потом
     * встаёт под кнопку.
     *
     * Раньше она ждала ответа `measureInWindow`, и открытие держалось
     * на предположении, что ответ вообще придёт. Проверялся при этом
     * только случай «метода нет» — а бывает хуже: метод есть, вызов
     * проходит, колбэк не приходит никогда (узел не привязан к
     * нативному дереву). Тогда нажатие не делает ровно ничего, и это
     * та самая мёртвая кнопка, ради которой запасной путь и писался.
     *
     * Скачка на экране нет: нативный замер отвечает в том же кадре, до
     * отрисовки. А если не ответит — карточка просто останется у
     * правого края, что человеку всё равно понятнее пустого нажатия.
     */
    setMenu({ top: FALLBACK_MENU_TOP, right: SCREEN_PADDING });

    if (typeof node?.measureInWindow !== 'function') return;

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
        onPress={() => setOpen(true)}
        accessibilityRole="button"
        accessibilityLabel={t('profile.language')}
        style={{
          position: 'absolute',
          right: BUTTON_RIGHT,
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
        visible={open}
        transparent
        animationType="fade"
        onRequestClose={() => setOpen(false)}
        // Без этого на Android окно начинается ПОД статус-баром, и
        // карточка встаёт ниже кнопки на его высоту.
        statusBarTranslucent
      >
        {/* Затемнение и оно же — «закрыть касанием мимо». */}
        <Pressable
          onPress={() => setOpen(false)}
          style={{ flex: 1, backgroundColor: 'rgba(0,0,0,0.55)' }}
        >
          {/* ⚠️ Обёртка с пустым обработчиком гасит касание по самой
              карточке: без неё нажатие на язык дошло бы и до затемнения,
              то есть закрыло бы карточку раньше выбора. */}
          <Pressable
            onPress={() => {}}
            style={{
              position: 'absolute',
              top: menuTop,
              right: BUTTON_RIGHT,
              width: MENU_WIDTH,
            }}
          >
            <View className="rounded-card-lg border border-border bg-surface-2 p-1.5">
              <LanguageSwitcher onSelect={() => setOpen(false)} />
            </View>
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
}
