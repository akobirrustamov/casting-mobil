// CRA bu faylni har bir test faylidan OLDIN yuklaydi.
//
// `@testing-library/jest-dom` DOM uchun qo'shimcha matcherlar beradi
// (`toBeInTheDocument` va h.k.). Usiz ular mavjud bo'lmaydi va test
// "is not a function" bilan yiqiladi.
import '@testing-library/jest-dom';
