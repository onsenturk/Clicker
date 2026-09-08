# FFmpeg LGPL Notice

StreamVault distributes a bundled Media3 FFmpeg audio decoder artifact built for LGPL-compatible use.

Distribution rules for this repo:

- Do not enable GPL or nonfree FFmpeg components in the bundled artifact.
- Keep the build provenance recorded in [the decoder manifest](../player/libs/media3-decoder-ffmpeg-1.11.0.properties).
- Preserve rebuild instructions so recipients can replace or relink the shipped decoder artifact if required by the applicable LGPL obligations.

The current decoder uses unmodified [Media3 1.11.0](https://github.com/androidx/media/tree/2bc207851df311340767e913931ca7b28cab1794/libraries/decoder_ffmpeg) and [FFmpeg 6.0.1](https://github.com/FFmpeg/FFmpeg/tree/c41ff724ede7da657762d61097e26fac296c53bf). The decoder AAR includes `META-INF/MEDIA3-FFMPEG-LICENSE.txt` and `META-INF/FFMPEG-LGPLv2.1.txt` inside its Java archive. See [FFmpeg Integration](FFMPEG.md) for the source revisions, enabled decoders, and rebuild procedure.

Operationally, this repo treats the FFmpeg AAR as a versioned bundled dependency rather than rebuilding it in CI.
