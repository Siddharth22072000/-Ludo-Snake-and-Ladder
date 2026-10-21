# 🎲 Ludo & Snakes and Ladders

**A modern multiplayer board game that combines the strategy of Ludo with the unpredictability of Snakes and Ladders.**

Experience a unique hybrid board game featuring real-time online multiplayer, private rooms, intelligent AI opponents, live chat, global leaderboards, and a beautifully animated Android interface.

Built with **Kotlin, Jetpack Compose, Firebase Cloud Firestore, and Material Design 3**, the game brings classic board-game fun to a modern, connected gaming experience.

---

## ✨ Features at a Glance

- 🎮 **Hybrid Gameplay:** Combines Ludo-inspired token mechanics with Snakes and Ladders progression.
- 🌐 **Online Multiplayer:** Quick Match and private rooms supporting 2–4 players.
- 🤖 **Intelligent AI Opponents:** Play against bots with heuristic-based decision-making.
- 📴 **Offline Modes:** Enjoy Pass & Play or practice against AI without an internet connection.
- 💬 **Real-Time Chat:** Send custom messages and quick reactions during online matches.
- 🏆 **Global Leaderboards:** Compete for podium positions and track your gaming performance.
- 🔐 **Google Sign-In:** Secure authentication with guest access as a fallback.
- 🎨 **Modern Android UI:** Animated dice, custom board rendering, vibrant tokens, and victory celebrations.

---

## 🎯 Game Overview

The game combines familiar elements from two classic board games into a single competitive experience.

### 🪜 Snakes and Ladders Mechanics

- Navigate a **10 × 10 serpentine board** containing 100 numbered tiles.
- Climb ladders to advance quickly toward the finish.
- Avoid snakes that send your tokens backward.
- Land on safe star tiles to benefit from designated safe-zone mechanics.

### 🎲 Ludo-Inspired Mechanics

- Manage multiple tokens during a match.
- Roll a six to bring a token out of Base.
- Earn an extra roll after rolling a six.
- Knock out opposing tokens using Ludo-inspired capture rules.
- Choose strategic moves based on token positions, available ladders, snakes, and opponents.

### 🏁 Victory Condition

Bring both of your tokens to **Tile 100** to win the match.

*The game combines Ludo-inspired token management and combat with the progression system of Snakes and Ladders.*

---

## 🌐 Online Multiplayer

Challenge friends or compete against other players with cloud-powered multiplayer features.

### ⚡ Quick Match — 1v1

- Enter matchmaking and find an opponent.
- Join active player lobbies.
- Start competitive matches without manually creating a room.

### 🔒 Custom Rooms — 2 to 4 Players

- Create private multiplayer rooms.
- Share room codes with friends.
- Join existing rooms using a room code.
- Enjoy matches with friends across supported devices.

### 🔄 Real-Time Synchronization

Online game state is synchronized using **Firebase Cloud Firestore real-time snapshot listeners**, allowing connected players to receive game updates as the match progresses.

---

## 💬 Real-Time Chat & Reactions

Keep every match interactive with built-in communication tools.

**Quick reactions include:**

| Reaction | Meaning |
|---|---|
| 🎉 | Celebration |
| 🐍 | Snake encounter |
| 🪜 | Ladder progress |
| 🔥 | Impressive move |
| 😅 | Close call |
| GG! | Good game |
| Nice roll! | Celebrate a dice roll |

Players can also send custom text messages with timestamps during online matches.

---

## 🤖 Play Against AI

Practice your strategy against AI-controlled opponents without requiring an online opponent.

Choose from matches against **1, 2, or 3 AI bots**, depending on the selected game mode.

The AI uses heuristic-based decision-making to prioritize:

- 🪜 Climbing ladders for faster progression.
- ⚔️ Knocking out opposing tokens when advantageous.
- 🐍 Avoiding dangerous moves that lead to snake traps.
- 🎲 Bringing tokens out of Base when a six is rolled.
- 🧠 Evaluating available moves to make better tactical decisions.

The objective is to make bot behavior more strategic than random dice-driven movement.

---

## 📴 Offline Pass & Play

Enjoy local multiplayer on a single device.

- Play turn by turn with friends or family.
- Share the same screen and device.
- No internet connection required.
- No online matchmaking required.

It's an ideal mode for casual gaming, travel, and quick matches.

---

## 🏆 Global Leaderboards & Player Profiles

Track progress and compete for a place among the best players.

### Leaderboard Highlights

- 🥇 Gold — First place
- 🥈 Silver — Second place
- 🥉 Bronze — Third place
- 🏆 Trophy-based ranking presentation
- 📊 Total wins and win-rate statistics

### Authentication

The game supports **Google Sign-In through Android Jetpack CredentialManager**, with guest access available as a fallback for supported offline gameplay.

Player profiles provide a foundation for tracking career statistics and competitive progress.

---

## 🎨 Modern Android Gaming Experience

The interface is designed to combine classic board-game familiarity with a modern gaming aesthetic.

### Custom 10 × 10 Board

- Serpentine tile numbering.
- Custom-rendered snakes and ladders.
- Three-dimensional golden ladder visuals.
- Safe star tiles.
- Distinctively colored player tokens.

### Animated Dice

- Three-dimensional rolling-dice presentation.
- Bounce animations.
- Animated dice pips.
- Interactive visual feedback during gameplay.

### Game Feedback

- Event notification banners.
- Victory celebration dialogs.
- Clear turn and game-state feedback.
- Adaptive launcher icons.
- Material Design 3 dark gaming theme.

The board and visual elements use custom Canvas rendering where appropriate, alongside Jetpack Compose UI components.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Primary Android development language |
| **Jetpack Compose** | Declarative UI development |
| **Custom Canvas Rendering** | Game board and graphical elements |
| **Material Design 3** | UI styling and design system |
| **Firebase Cloud Firestore** | Real-time multiplayer synchronization |
| **Google Sign-In** | Player authentication |
| **Jetpack CredentialManager** | Modern Android credential integration |
| **Heuristic-Based AI** | Automated opponent decision-making |
| **Kotlin Coroutines / Flow** | Asynchronous processing and reactive state, where implemented |

---

## 🚀 Getting Started

### Prerequisites

Before running the project, make sure you have:

- Android Studio installed.
- A compatible Android SDK.
- A suitable JDK version for the project's Gradle configuration.
- Git installed.
- A Firebase project configured for the application if you want to use online multiplayer and cloud-backed features.

### 1. Clone the Repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd <YOUR_PROJECT_DIRECTORY>
```

Replace the placeholders with your actual GitHub repository URL and project directory.

### 2. Open the Project

1. Launch Android Studio.
2. Select **Open** and choose the cloned project directory.
3. Allow Gradle synchronization to finish.
4. Resolve any SDK or dependency prompts displayed by Android Studio.

### 3. Configure Firebase

For online multiplayer and authentication:

1. Create or select a project in the Firebase Console.
2. Register your Android application using its actual application ID.
3. Add the generated `google-services.json` file to the appropriate app module directory.
4. Enable Cloud Firestore and configure the required database.
5. Configure Google authentication and the associated SHA fingerprints as required.
6. Set up appropriate Firestore security rules before deploying the application.

**Security note:** Never use permissive Firestore rules in production. Validate player identity, room membership, legal moves, turn order, and victory conditions on a trusted backend where appropriate.

### 4. Build and Run

Use Android Studio to build and run the application on an emulator or compatible Android device.

Alternatively, use the Gradle wrapper:

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

The exact setup requirements may vary depending on the project's Gradle, Android Gradle Plugin, and SDK versions.

---

## 🎮 Available Game Modes

| Mode | Players | Internet Required |
|---|---:|---|
| Quick Match | 2 | Yes |
| Custom Room | 2–4 | Yes |
| Play vs AI | 1 player + 1–3 bots | No |
| Pass & Play | Local players | No |

*Online features require appropriate Firebase configuration and connectivity. Guest and offline behavior depends on the implementation of the selected mode.*

---

## 📂 Project Structure

The following is a suggested logical organization. Actual directory names may differ depending on the repository structure.

```text
app/
├── src/
│   └── main/
│       ├── java/
│       │   └── <package-name>/
│       │       ├── game/          # Game rules and turn management
│       │       ├── ai/            # AI move evaluation
│       │       ├── data/          # Data sources and repositories
│       │       ├── multiplayer/   # Matchmaking and room management
│       │       ├── auth/          # Authentication
│       │       ├── ui/            # Compose screens and components
│       │       └── MainActivity.kt
│       └── res/                   # Android resources and launcher icons
├── build.gradle.kts
└── google-services.json           # Firebase configuration, if required
```

**Note:** This tree illustrates a possible architecture and should not be treated as a verified listing of the repository's current files.

---

## 🔐 Security & Reliability

For a robust multiplayer experience, consider the following production safeguards:

- Enforce Firestore security rules for authenticated users and room membership.
- Validate moves against the authoritative game state.
- Prevent duplicate turn submissions and invalid token movement.
- Verify match completion and leaderboard updates.
- Restrict access to private-room messages and game data.
- Handle reconnection, stale sessions, and interrupted matches.
- Avoid storing credentials, private keys, or sensitive configuration in source control.

Client-side validation improves the experience, but it should not be the sole protection for competitive online gameplay.

---

## 🗺️ Potential Future Enhancements

Possible additions for future releases include:

- 🎚️ Adjustable AI difficulty levels.
- 🏅 Seasonal tournaments and ranked matchmaking.
- 👥 Friends lists and player invitations.
- 📈 Detailed match history and statistics.
- 🎵 Background music and sound effects.
- 🌍 Localization and multilingual support.
- 🔁 Match reconnection and resumable sessions.
- 🧪 Automated unit tests for game rules and AI decisions.
- 🛡️ Server-authoritative multiplayer validation.

These are potential improvements, not claims about currently implemented functionality.

---

## 🤝 Contributing

Contributions, suggestions, and bug reports are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Implement and test your changes.
4. Commit your work with a descriptive message.
5. Open a pull request explaining the changes.

For substantial changes, consider opening an issue first to discuss the proposed implementation.

---

## 📜 License

Add the license that applies to this project, such as MIT or Apache-2.0, after confirming the intended terms of distribution.

If the repository does not yet contain a license, the project should not be assumed to be open-source licensed merely because it is publicly available on GitHub.

---

## ⭐ Support the Project

If you enjoy this hybrid board-game experience, consider giving the repository a **⭐ Star** on GitHub.

Share it with friends, challenge your opponents, climb the ladders, avoid the snakes, and race your tokens to Tile 100!

**Roll the dice. Make your move. Own the board.** 🎲🐍🪜
