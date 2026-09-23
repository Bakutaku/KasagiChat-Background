# 本番（ECS Express Mode）用のコンテナイメージ。
# jar は GitHub Actions で `./gradlew bootJar` 済みのものを入れ、イメージ内ではビルドしない。
# ローカルで試す場合も、先に bootJar を実行してから docker build する。
FROM eclipse-temurin:25-jre

WORKDIR /app
COPY build/libs/*.jar app.jar

# アプリが乗っ取られた場合の被害を抑えるため、root 以外のユーザーで実行する。
USER 1000:1000

EXPOSE 8080

# ヒープの上限をタスクのメモリ量から決める（2GB なら約1.5GB）。固定値にするとタスクのサイズ変更時に追従しない。
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
