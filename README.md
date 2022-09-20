# Remote Python Plugin

[![IDEA version][1]][7]
[![Latest release][2]][3]
[![Test status][4]][5]


<!-- Plugin description -->

The **Remote Python** plugin provides [Run Configurations][6] for running remote Python commands in 
a [JetBrains][7] IDE that does not have built-in remote Python support, *e.g.* [CLion][8]. SSH
hosts, [Docker][9] containers, and [Vagrant][10] machines can be used as remote environments.

This is not a substitute for the remote interpreter support in [PyCharm Professional][11], and does 
not support remote development features.


[6]: https://www.jetbrains.com/help/idea/run-debug-configuration.html
[7]: https://www.jetbrains.com
[8]: https://www.jetbrains.com/clion
[9]: https://docker.com
[10]: https://vagrantup.com
[11]: https://www.jetbrains.com/pycharm

<!-- Plugin description end -->

## Installation

[GitHub releases][3] include a binary distribution named`idea-remote-plugin-<version>.zip` that can 
be used to [install the plugin from disk][12].


[1]: https://img.shields.io/static/v1?label=IDEA&message=2022.1%2B&color=informational
[2]: https://img.shields.io/github/v/release/mdklatt/idea-remotepython-plugin?sort=semver
[3]: https://github.com/mdklatt/idea-remotepython-plugin/releases
[4]: https://github.com/mdklatt/idea-remotepython-plugin/actions/workflows/test.yml/badge.svg
[5]: https://github.com/mdklatt/idea-remotepython-plugin/actions/workflows/test.yml
[12]: https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk
