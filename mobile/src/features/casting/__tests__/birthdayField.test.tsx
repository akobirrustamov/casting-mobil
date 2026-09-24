/**
 * Календарь даты рождения: выбор года → дня отдаёт `ДД.ММ.ГГГГ`,
 * а будущие дни выбрать нельзя — `parseBirthday` их всё равно отклонит,
 * и человек не понял бы, почему дата «неверная».
 */
import { act, create, type ReactTestInstance, type ReactTestRenderer } from 'react-test-renderer';
import { Text } from 'react-native';

import { BirthdayField } from '../BirthdayField';

jest.mock('@expo/vector-icons', () => ({ Ionicons: 'Ionicons' }));
jest.mock('react-native-safe-area-context', () => ({
  useSafeAreaInsets: () => ({ top: 0, bottom: 0, left: 0, right: 0 }),
}));
jest.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) =>
      key === 'casting.form.calendar.months'
        ? ['Yan', 'Fev', 'Mar', 'Apr', 'May', 'Iyn', 'Iyl', 'Avg', 'Sen', 'Okt', 'Noy', 'Dek']
        : key === 'casting.form.calendar.weekdays'
          ? ['Du', 'Se', 'Ch', 'Pa', 'Ju', 'Sh', 'Ya']
          : key,
  }),
}));

function pressableWithText(tree: ReactTestRenderer, text: string | number): ReactTestInstance {
  // Последнее совпадение — самая глубокая кнопка: подложка шторки
  // тоже нажимается и тоже содержит все эти тексты.
  const nodes = tree.root.findAll(
    (n) => typeof n.props?.onPress === 'function' && n.findAllByType(Text).some((t) => String(t.props.children) === String(text)),
  );
  return nodes[nodes.length - 1];
}

function openField(tree: ReactTestRenderer) {
  const field = tree.root.find((n) => typeof n.props?.onPress === 'function' && n.props.accessibilityLabel === 'Sana');
  act(() => field.props.onPress());
}

describe('BirthdayField', () => {
  it('выбор года и дня отдаёт дату ДД.ММ.ГГГГ', () => {
    const onChange = jest.fn();
    let tree!: ReactTestRenderer;
    act(() => {
      tree = create(<BirthdayField label="Sana" value="" onChange={onChange} placeholder="Tanlang" />);
    });

    // Пустое поле открывается сразу на списке годов.
    openField(tree);
    act(() => pressableWithText(tree, 2001).props.onPress());
    act(() => pressableWithText(tree, 5).props.onPress());

    expect(onChange).toHaveBeenCalledWith('05.01.2001');
  });

  it('будущие дни недоступны', () => {
    const today = new Date();
    const value = `01.${String(today.getMonth() + 1).padStart(2, '0')}.${today.getFullYear()}`;
    let tree!: ReactTestRenderer;
    act(() => {
      tree = create(<BirthdayField label="Sana" value={value} onChange={jest.fn()} />);
    });
    openField(tree);

    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate();
    if (lastDay > today.getDate()) {
      expect(pressableWithText(tree, lastDay).props.disabled).toBe(true);
    }
    expect(pressableWithText(tree, 1).props.disabled).toBe(false);
  });
});
