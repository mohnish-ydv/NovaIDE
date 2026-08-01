package com.mohnishraj.novaide.templates

data class TemplateFile(val path: String, val content: String)
data class ProjectTemplate(val id: String, val name: String, val description: String, val files: List<TemplateFile>)

object TemplateCatalog {
    val all: List<ProjectTemplate> = listOf(
        ProjectTemplate(
            id = "web",
            name = "Modern Web App",
            description = "Responsive HTML, CSS and JavaScript starter",
            files = listOf(
                TemplateFile(
                    "index.html",
                    """<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Nova Project</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <main>
    <h1>Built with NovaIDE</h1>
    <p>Edit app.js to begin.</p>
  </main>
  <script src="app.js"></script>
</body>
</html>
"""
                ),
                TemplateFile(
                    "styles.css",
                    """* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-height: 100vh;
  display: grid;
  place-items: center;
  font-family: system-ui, sans-serif;
  background: #0b1220;
  color: #eef4ff;
}

main {
  padding: 2rem;
  text-align: center;
}
"""
                ),
                TemplateFile("app.js", "console.log('NovaIDE project ready');\n"),
                TemplateFile("README.md", "# Modern Web App\n\nCreated with NovaIDE.\n")
            )
        ),
        ProjectTemplate(
            id = "phaser",
            name = "Phaser Game",
            description = "Landscape-ready Phaser 3 starter",
            files = listOf(
                TemplateFile(
                    "index.html",
                    """<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Nova Phaser Game</title>
  <style>
    html, body { margin: 0; min-height: 100%; background: #050914; }
    canvas { display: block; margin: auto; }
  </style>
</head>
<body>
  <script src="https://cdn.jsdelivr.net/npm/phaser@3.90.0/dist/phaser.min.js"></script>
  <script src="src/game.js"></script>
</body>
</html>
"""
                ),
                TemplateFile(
                    "src/game.js",
                    """const config = {
  type: Phaser.AUTO,
  width: 960,
  height: 540,
  backgroundColor: '#081120',
  scale: {
    mode: Phaser.Scale.FIT,
    autoCenter: Phaser.Scale.CENTER_BOTH
  },
  scene: {
    create() {
      this.add.text(480, 270, 'NovaIDE Phaser', {
        fontSize: '44px',
        color: '#ffffff'
      }).setOrigin(0.5);
    }
  }
};

new Phaser.Game(config);
"""
                ),
                TemplateFile("README.md", "# Phaser Game\n\nOpen index.html in a browser.\n")
            )
        ),
        ProjectTemplate(
            id = "python",
            name = "Python Utility",
            description = "CLI-ready Python starter",
            files = listOf(
                TemplateFile(
                    "main.py",
                    """def main() -> None:
    print("NovaIDE Python project ready")


if __name__ == "__main__":
    main()
"""
                ),
                TemplateFile("requirements.txt", ""),
                TemplateFile("README.md", "# Python Utility\n")
            )
        ),
        ProjectTemplate(
            id = "node",
            name = "Node.js CLI",
            description = "Dependency-light Node.js command-line starter",
            files = listOf(
                TemplateFile(
                    "package.json",
                    """{
  "name": "nova-node-app",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "start": "node src/index.js"
  }
}
"""
                ),
                TemplateFile("src/index.js", "console.log('NovaIDE Node project ready');\n"),
                TemplateFile("README.md", "# Node.js CLI\n")
            )
        )
    )
}
