package us.obviously.itmo.prog.commands.argument_validators;

import us.obviously.itmo.prog.exceptions.InvalidArgumentException;
import us.obviously.itmo.prog.manager.Management;

/**
 * Проверка, что пользователь передал верное значение в качестве аргумента
 *
 * @param <T> Тип ввода пользователя
 * @param <U> Возвращаемый тип (реальный тип объекта)
 */
abstract class AbstractArgumentValidator<T, U> {
    Management manager;

    AbstractArgumentValidator(Management manager) {
        this.manager = manager;
    }

    /**
     * @param value Сырой ввод пользователя
     * @return Желаемый объект
     * @throws InvalidArgumentException Выбросит исключение, если пользователь ввёл невалидное значение
     */
    abstract public U validate(T value) throws InvalidArgumentException;
}
