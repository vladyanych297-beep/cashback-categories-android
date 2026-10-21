# Сертификат HTTPS для Т-Банка

Для доступа Android WebView к официальному сайту Т-Банка используется дополнительный корневой сертификат Russian Trusted Root CA. Доверие ограничено доменом `tbank.ru` и его поддоменами в `res/xml/network_security_config.xml`. Остальные сайты используют стандартное системное хранилище Android. Сертификаты пользователя и устройства не устанавливаются и не изменяются.

- [Официальная инструкция Т-Банка](https://www.tbank.ru/bank/help/certificates/).
- [Официальный файл из инструкции](https://help-static2.tbank-online.com/certs/linux_russian_trusted_root_ca_pem/russian_trusted_root_ca_pem.crt).
- Файл приложения: `res/raw/russian_trusted_root.pem`.
- SHA-256 DER-сертификата: `d26d2d0231b7c39f92cc738512ba54103519e4405d68b5bd703e9788ca8ecf31`.
- Субъект и издатель: `CN=Russian Trusted Root CA, O=The Ministry of Digital Development and Communications, C=RU`.
- Алгоритм: RSA 4096 / SHA256withRSA; окончание срока действия — 28 февраля 2032 года.

Файл скачан по HTTPS без отключения проверки сертификатов. Самоподпись и отпечаток проверяются в `tests/tbank-scripts.cjs`. Приложение не вызывает `SslErrorHandler.proceed()`; при ошибке проверки соединение отклоняется. При обновлении сертификата необходимо сверить новый официальный источник, отпечаток, срок действия и подпись, затем обновить тест.
