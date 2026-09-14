import { Ionicons } from '@expo/vector-icons';
import { Image } from 'expo-image';
import { Pressable, Text, View } from 'react-native';

import { colors } from '@/theme/tokens';

import { Badge } from './Badge';

/**
 * Карточка контента: кадр с бейджем в углу и подпись под ним.
 *
 * По умолчанию постер 2:3 — паттерн из Yangi.TV, бейджи по ТЗ
 * (locked / purchased вместо PREMIUM / Bepul).
 *
 * Пропорция вынесена наружу, но во всём приложении она одна
 * (`features/content/railLayout.CARD_RATIO`): ряд, сетка и экран
 * «Barchasi» показывают карточку одного размера — заказчик 01.09.2026.
 */
export type PosterBadge = 'premiere' | 'locked' | 'purchased' | null;

/**
 * Высота строки подписи и строк помельче — `lineHeight` классов
 * `text-caption` и `text-micro` из `tailwind.config.js`.
 *
 * ⚠️ Числа продублированы здесь, потому что раскладке нужна ВЫСОТА, а её
 * из класса не прочитать. Поменяется `lineHeight` в конфиге — поменять и
 * тут, иначе подписи снова разъедутся по уровням.
 */
const CAPTION_LINE = 18;
const MICRO_LINE = 14;

/** Сколько строк отведено названию — столько же места у любого названия. */
const TITLE_LINES = 2;

export function PosterCard({
  title,
  subtitle,
  imageUrl,
  badge = null,
  badgeLabel,
  /** Знак в бейдже — пламя на «премьере». */
  badgeIcon,
  /**
   * Просмотры — левый НИЖНИЙ угол кадра. Уже отформатированы вызывающим.
   *
   * ⚠️ Счётчик стоит НА кадре, а не четвёртой строкой под подписью:
   * подпись растянула бы карточку, а её высота одна на всё приложение
   * (`railLayout.CARD_RATIO`) и утверждена заказчиком 01.09.2026.
   * Строка выросла бы разом в рядах, сетке и на «Barchasi».
   *
   * ⚠️ «Нравится» здесь НЕТ: карточка в ряду ~105px шириной. Сердце —
   * кнопка, и живёт на экране контента.
   */
  views,
  /** Третья строка: жанр. */
  meta,
  /**
   * Меню карточки — знак «⋮» в правом верхнем углу кадра (макет «Media»).
   *
   * Необязательный: в РЯДАХ на главной меню нет. Там карточка узкая и
   * стоит вплотную к соседней, и знак поверх кадра ловил бы нажатия,
   * которыми люди листают ряд вбок.
   */
  onMenu,
  /** Подпись знака «⋮» для озвучки. Переводит вызывающий — здесь нет i18n. */
  menuLabel,
  width = 132,
  /** Ширина к высоте кадра. См. `features/content/railLayout.CARD_RATIO`. */
  ratio = 2 / 3,
  onPress,
  progressPercent,
}: {
  title: string;
  subtitle?: string;
  imageUrl?: string;
  badge?: PosterBadge;
  badgeLabel?: string;
  badgeIcon?: keyof typeof Ionicons.glyphMap;
  views?: string;
  meta?: string;
  onMenu?: () => void;
  menuLabel?: string;
  width?: number;
  ratio?: number;
  onPress?: () => void;

  /**
   * Полоса досмотра внизу обложки, 0–100. Без неё карточка обычная.
   *
   * ⚠️ Живёт ЗДЕСЬ, а не в отдельной карточке для «продолжить»: форма
   * кадра одна на всё приложение, и второй компонент со временем
   * разошёлся бы с этим по размеру, скруглению и подписям — а стоят
   * они на одном экране, друг под другом.
   *
   * `null`/`undefined` — длительность неизвестна: полосу рисовать не
   * из чего. Ноль в этом случае был бы ложью — «не начинал», хотя
   * человек мог посмотреть половину.
   */
  progressPercent?: number | null;
}) {
  return (
    <Pressable style={{ width }} onPress={onPress} className="gap-2 active:opacity-70">
      <View
        style={{ width, height: Math.round(width / ratio) }}
        className="overflow-hidden rounded-card bg-surface-2"
      >
        {imageUrl ? (
          <Image
            source={{ uri: imageUrl }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            transition={200}
          />
        ) : null}

        {/*
          Просмотры — левый нижний угол кадра.

          ⚠️ Раньше правый угол занимал таймкод, и на узкой карточке ряда
          (около 120 точек) две метки сходились в середине: «1 234 567» и
          «1:23:45» в такую ширину просто не помещаются. Держала их врозь
          общая строка с `justify-between`.

          Заказчик (14.09.2026) таймкод убрал совсем — «video davomiyligi
          qiymati ko'rsatish olib tashlash kerak, kerak emas», — поэтому
          разводить больше нечего и строка снова одиночная.
        */}
        {views ? (
          <View className="absolute bottom-2 left-2 max-w-[70%] flex-row items-center gap-1 rounded-pill bg-ink/70 px-2 py-0.5">
            <Ionicons name="eye-outline" size={11} color={colors.white} />
            <Text numberOfLines={1} className="text-micro font-semibold text-text">
              {views}
            </Text>
          </View>
        ) : null}

        {onMenu ? (
          // Знак без подложки, как на макете. Тень под ним — чтобы белое
          // не пропадало на светлом кадре: подложка в этом углу спорила бы
          // с меткой доступа слева.
          <Pressable
            onPress={onMenu}
            accessibilityRole="button"
            accessibilityLabel={menuLabel}
            hitSlop={10}
            className="absolute right-1 top-1 h-7 w-7 items-center justify-center active:opacity-60"
          >
            <Ionicons
              name="ellipsis-vertical"
              size={16}
              color={colors.white}
              style={{ textShadowColor: 'rgba(0,0,0,0.65)', textShadowRadius: 6 }}
            />
          </Pressable>
        ) : null}

        {/* Полоса досмотра — по нижнему краю кадра, во всю ширину.
            Счётчик просмотров сидит чуть выше неё в том же углу и не
            перекрывается: полоса тонкая и прижата к самому краю. */}
        {typeof progressPercent === 'number' ? (
          <View className="absolute bottom-0 left-0 right-0 h-1 bg-ink/60">
            <View
              style={{ width: `${Math.min(100, Math.max(0, progressPercent))}%` }}
              className="h-full bg-magenta"
            />
          </View>
        ) : null}

        {badge && badgeLabel ? (
          // Левый верхний угол — как на макете заказчика. Справа он налезал
          // на лица: у постеров герой обычно смещён вправо.
          <View className="absolute left-2 top-2">
            <Badge tone={badge} icon={badgeIcon} translucent>
              {badgeLabel}
            </Badge>
          </View>
        ) : null}
      </View>

      {/*
        Подпись: три строки на ПОСТОЯННЫХ местах.

        Заказчик (14.09.2026, скриншот с обведёнными «Drama», «1 qism» и
        «Romantika»): «janr nomlari va qism raqamlari o'zgarmas bir xil
        sathda bo'lishi kerak, media nomiga qarab balandligi o'zgarmasin».

        <h2>Что было не так</h2>
        Строки просто шли друг за другом, и каждая ехала за предыдущей:
        у названия в одну строку жанр поднимался на 18 точек выше, чем у
        соседа с названием в две; а если у контента не было ни числа
        серий, ни описания, жанр подскакивал ещё на строку. В ряду из трёх
        карточек жанры стояли на трёх разных уровнях.

        <h2>Почему рамки, а не `minHeight` на тексте</h2>
        Место занимает ОБЁРТКА, а текст лежит внутри по верхнему краю.
        `height` на самом `<Text>` Android и iOS отрабатывают по-разному
        (второй прижимает строку к середине отведённой высоты), и одна и
        та же карточка выглядела бы на двух телефонах по-разному.

        ⚠️ Пустая строка тоже занимает место. Иначе «постоянный уровень»
        держался бы только у карточек с полным набором подписей — то
        есть ровно до первого контента без жанра.
      */}
      <View>
        <View style={{ height: TITLE_LINES * CAPTION_LINE }}>
          <Text numberOfLines={TITLE_LINES} className="text-caption text-text">
            {title}
          </Text>
        </View>

        <View style={{ height: MICRO_LINE }}>
          {subtitle ? (
            <Text numberOfLines={1} className="text-micro text-text-muted">
              {subtitle}
            </Text>
          ) : null}
        </View>

        {/* Жанр отдельной строкой и фирменным фиолетовым — на макете
            «Media» это акцент под подписью, а не третий серый уровень. */}
        <View style={{ height: MICRO_LINE }}>
          {meta ? (
            <Text numberOfLines={1} className="text-micro text-violet">
              {meta}
            </Text>
          ) : null}
        </View>
      </View>
    </Pressable>
  );
}
