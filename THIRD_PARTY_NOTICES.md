# Third-Party Notices

HardWire is distributed under the MIT License (see `LICENSE`). It also contains
or depends on the following third-party software.

## AdbLib

The ADB protocol implementation under
`app/src/main/java/com/hitomatito/hardwire/adblib/` is derived from
[AdbLib](https://github.com/cgutman/AdbLib) by Cameron Gutman.

```text
Copyright (c) 2013, Cameron Gutman
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

  Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

  Neither the name of the copyright holder nor the names of its contributors may
  be used to endorse or promote products derived from this software without
  specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## AndroidX, Jetpack Compose, Material Components, Kotlin and Kotlinx

HardWire uses AndroidX, Jetpack Compose, Material 3, Kotlin and Kotlinx
libraries distributed under the Apache License 2.0. The corresponding notices
and license texts are available from each project's published artifacts and
source repositories:

- https://developer.android.com/jetpack/androidx
- https://github.com/JetBrains/kotlin
- https://github.com/Kotlin/kotlinx.coroutines
- https://github.com/Kotlin/kotlinx.serialization

## SoC data

The built-in SoC name table is maintained in
`app/src/main/java/com/hitomatito/hardwire/data/command/CommandParser.kt`.
No external SoC lookup service is used at runtime.
