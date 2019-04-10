# ImgLib2 Label Multisets [![Build Status](https://travis-ci.org/saalfeldlab/imglib2-label-multisets.svg?branch=master)](https://travis-ci.org/saalfeldlab/imglib2-label-multisets)

Efficient implementation of label multisets as an ImgLib2 `NativeType` backed by primitive arrays.

Provides:
* Type classes `LabelMultisetType` and `VolatileLabelMultisetType`
* Conversion to/from `byte[]`
* Abstract cache loader for reading cells of `LabelMultisetType`
