# カラス印のチームユーティリティプラグイン

# 概要

このプラグインは、導入することで、観戦チームと参加チームに分け、
コマンドでチームをリフレッシュすることができます

# 導入方法

1. プラグインをダウンロードします
2. `plugins`フォルダに入れます
3. サーバーを再起動します
4. `/kteam`コマンドが使えるようになります

# コマンド一覧

| コマンド                               | 説明                                    |
|------------------------------------|---------------------------------------|
| `/kteam clear`                     | すべてのチームをクリアしてリフレッシュします              |
| `/kteam refresh <team member size>` | 指定したサイズでチームを再編成します                  |
| `/kteam viewers add <player>`      | プレイヤーを観戦チームに追加します                   |
| `/kteam viewers remove <player>`   | 観戦チームから指定したプレイヤーを削除します              |
| `/kteam viewers list`              | 現在の観戦チームに所属するプレイヤー一覧を表示します          |
| `/kteam players add <player>`      | プレイヤーを参加チームに追加します                   |
| `/kteam players remove <player>`   | 参加チームから指定したプレイヤーを削除します              |
| `/kteam players list`              | 現在の参加チームに所属するプレイヤー一覧を表示します          |
| `/kteam rules <rule> <value>`      | 指定したルールの値を設定します                     |
| `/kteam rules list`                | 現在設定されているルール一覧を表示します               |
| `/kteam help`                      | コマンドのヘルプを表示します                      |
| `/kteam version`                   | プラグインのバージョン情報を表示します                 |
| `/kteam reload`                    | プラグイン全体を再読み込みします                    |
| `/kteam reload config`             | プラグインの設定ファイルのみを再読み込みします            |
| `/kteam reload messages`           | プラグインのメッセージファイルのみを再読み込みします         |
| `/kteam reload teams`              | チーム構成のみを再読み込みします                    |
| `/kteam reload rules`              | ゲームルール設定のみを再読み込みします                 |
| `/kteam help <page>`               | ヘルプの特定ページを表示します                     |
