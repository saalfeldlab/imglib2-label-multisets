# ImgLib2 Label Multisets

Efficient implementation of label multisets as an ImgLib2 `NativeType` backed by primitive arrays.

Provides:
* Type classes `LabelMultisetType` and `VolatileLabelMultisetType`
* Conversion to/from `byte[]`
* Abstract cache loader for reading cells of `LabelMultisetType`
