NeuroVizJ
=========

An image processing application for making it easy for researchers
to perform cell tracking and data collection.

This project is based off of NeuroViz and is developed in Java, using
ImageJ as an image processing library.

Temporarily using Matlab for robustness (will be phased out in future iterations)

Usage
=====

`-i input_name` -- Specifies the name of the input file. Default is "input.png"
`-o output_name` -- Specifies the name of the output file. Default is "output.png"
`-s` -- Specifies that we will perform segmentation on the input image.
`-f folder` -- Specifies the folder to look for images to track (when tracking is enabled).
`-t` -- Specifies that we will track a sequence of images located in the folder defined by -f
`-thresh` -- Specifies that we want to return a thresholded image