# Döngü Android güncelleme imzası

Döngü 1.1.0 ve sonraki sürümlerin mevcut kurulumun üstüne güncellenebilmesi için aynı Android imza anahtarı kullanılmalıdır.

## GitHub Actions secrets

Repository > Settings > Secrets and variables > Actions bölümüne şu dört secret eklenir:

- `DONGU_KEYSTORE_BASE64`
- `DONGU_KEYSTORE_PASSWORD`
- `DONGU_KEY_ALIAS`
- `DONGU_KEY_PASSWORD`

Değerler özel olarak saklanan `Dongu-Signing-Kit-KEEP-PRIVATE.zip` içindeki `GITHUB-SECRETS.txt` dosyasında bulunur. Bu dosyalar public repoya kesinlikle yüklenmemelidir.

Workflow secrets mevcutsa `assembleRelease` çıktısını `apksigner` ile kalıcı anahtarla imzalayıp `Dongu-APK` artifact'ını üretir. Secrets yoksa yalnızca `Dongu-Test-APK-NON-UPDATABLE` ve imzasız release artifact'ı üretilir.

Her yeni sürümde `versionCode` mutlaka artırılmalıdır. `versionName` kullanıcıya görünen sürümdür.

> Not: İlk 1.0.0 debug APK geçici GitHub debug anahtarıyla imzalandığı için 1.1.0 kalıcı anahtara geçişinde bir kez kaldırıp yeniden kurmak gerekir. 1.1.0 ve sonrası aynı kalıcı anahtarla doğrudan güncellenebilir.
