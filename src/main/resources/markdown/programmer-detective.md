# Программист-детектив

<div class="article-publication-date">
    <time datetime="2023-08-10 20:23">10.08.2023 20:23</time>
</div>

Я работаю в компании, продукт которой устанавливается на компьютеры компаний-заказчиков. У нас нет доступа к их системам, мы не можем в случае неисправностей как-то подключиться к ним, исправить что-то в базе данных или провести пользователя «за ручку», чтобы починить систему. Вместо этого в случае поломок нам присылают страшные вещи — «логи».

Логи — это сообщения от программы, которые она пишет в процессе своей работы. Логи заложены в программу разработчиками, чтобы понять, что именно она делала и с какими данными работала в тот или иной момент времени.

Когда компания-заказчик сталкивается с проблемой в нашем продукте, мы первым делом просим открыть настройки и разрешить запись всех логов (вообще всех). И вот тут-то и начинается самое интересное: работа с ними очень сильно напоминает работу детектива при расследовании преступления.

Итак, у нас есть преступление — ошибка программы — которое мы расследуем. Иногда оно сопровождается убийствами: падение программы, потеря данных. У нас есть свидетели: сотрудники компании-заказчика, инженеры технической поддержки, которые видели воспроизведение проблемы. У нас есть обстоятельства — условия, при которых возникла ошибка (например, версия нашего продукта). Конечно, у нас есть улики — те самые логи. И, в конце концов, у нас есть подозреваемый — наш продукт, его код, который, конечно же, будет оправдываться и считать себя невиновным, исправно работающим. 

Методы расследования «преступлений» у нас тоже как у настоящих детективов. Мы проводим следственные эксперименты: пытаемся воспроизвести проблемы на своих устройствах. Мы выдвигаем гипотезы и ищем им подтверждение: «а что, если сломалось здесь, тогда программа пойдёт так и... а нет, этих логов нет». Разве что не хватает пробковой доски с фотографиями и нитками — пока что обходимся текстовыми документами.

И когда преступник-ошибка находится, на него заводится дело — задача «постмортем» (дословно, «посмертная») или отписка в задаче технической поддержки — о том, как этот преступник себя проявляет, как мы его перевоспитали (исправили ошибку) и что делать, если что-то похожее появится опять. А оно появится. Этот город никогда не спит...