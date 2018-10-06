# Gradle Apt Repository Plugin

[![JitPack](https://jitpack.io/v/Jartreg/gradle-apt-repo.svg)](https://jitpack.io/#Jartreg/gradle-apt-repo)
[![GitHub license](https://img.shields.io/github/license/Jartreg/gradle-apt-repo.svg)](https://github.com/Jartreg/gradle-apt-repo/blob/master/LICENSE)

This plugin provides creation of APT Repositories using Gradle. The repositories are created in the [default Debian repository format](https://wiki.debian.org/DebianRepository/Format), which means they can contain different releases and components and are signed by `InRelease` and `Release.gpg` files. The whole plugin is implemented in Kotlin and does not require any external binaries.