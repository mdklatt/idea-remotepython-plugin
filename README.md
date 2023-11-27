# IDEA Remote Python Plugin

[![IDEA version][1]][7]
[![Latest release][2]][3]
[![Test status][4]][5]

## Description

<!-- This content is used by the Gradle IntelliJ Plugin. --> 
<!-- Plugin description -->

[Run Configurations][6] for running remote Python commands in a [JetBrains][7]
IDE that does not have built-in remote Python support. This is not a substitute 
for the full remote interpreter support in [PyCharm Professional][11] and does
not support remote development features like code completion or debugging.

Each remote interpreter type has its own run configuration:

### Secure Shell (SSH)

Use an [OpenSSH][8]-compatible client on the local machine to execute on a 
remote host via SSH. The client configuration is used for connections (host
addresses, proxy settings, SSH keys, *etc*.)

### Docker

Use the [Docker][9] runtime on the local machine to execute inside a running
container, or a temporary container created from an image or [Compose][13]
service.

### Vagrant

Use the [Vagrant][10] executable on the local machine to execute inside a 
virtual machine.



[6]: https://www.jetbrains.com/help/idea/run-debug-configuration.html
[7]: https://www.jetbrains.com
[8]: https://www.openssh.com
[9]: https://docker.com
[10]: https://vagrantup.com
[11]: https://www.jetbrains.com/pycharm
[13]: https://docs.docker.com/compose

<!-- Plugin description end -->

## Installation

[Releases][3] include a binary distribution named `idea-remotepython-plugin-<version>.zip` 
that can be used to [install the plugin from disk][12].

## Installation

The latest version is available via a [custom plugin repository][14]. [Releases][3]
include a binary distribution named `idea-remotepython-plugin-<version>.zip` that
can be [installed from disk][12].


[1]: https://img.shields.io/static/v1?label=IDEA&message=2023.1%2B&color=informational
[2]: https://img.shields.io/github/v/release/mdklatt/idea-remotepython-plugin?sort=semver
[3]: https://github.com/mdklatt/idea-remotepython-plugin/releases
[4]: https://github.com/mdklatt/idea-remotepython-plugin/actions/workflows/test.yml/badge.svg
[5]: https://github.com/mdklatt/idea-remotepython-plugin/actions/workflows/test.yml
[12]: https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk
[14]: https://mdklatt.github.io/idea-plugin-repo
