# ImgLib2 Label Multisets [![Build Status](https://github.com/saalfeldlab/imglib2-label-multisets/actions/workflows/build-main.yml/badge.svg)](https://github.com/saalfeldlab/imglib2-label-multisets/actions/workflows/build-main.yml)

Efficient implementation of label multisets as an ImgLib2 `NativeType` backed by primitive arrays.

Provides:
* Type classes `LabelMultisetType` and `VolatileLabelMultisetType`
* Conversion to/from `byte[]`
* Abstract cache loader for reading cells of `LabelMultisetType`
