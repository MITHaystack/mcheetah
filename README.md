# MCheetah
- Create data processing pipelines on Android mobile phones
- Easily connect your data I/O to cloud environments like Dropbox
- Skip detailed Android programming; just connect your pipeline stages using producer-consumer patterns
- Create data processing fragment abstractions using 'Parsers', 'Computers', and 'Renderers'
- Leverage GPU-optimized plotting with OpenGL for large data sets
- Enjoy auto-tuning of threads to optimize speed on a variety of mobile architectures
- Enable approximate computing to optimize apps for speed, accuracy, or energy consumption
- Easily reuse your critical code for desktop Java applications
- Open spource (MIT license)

<p align="center">
  <img alt="MCheetah Overview" src="https://github.com/MITHaystack/mcheetah/blob/master/docs/images/mcheetah_overviewdiag.png" width="860"/>
</p>

### Documentation

- Code documentation (JavaDoc): [/docs/index.html](https://htmlpreview.github.io/?https://raw.githubusercontent.com/MITHaystack/mcheetah/master/docs/overview-summary.html)


### Contributors

Project lead: [Victor Pankratius (MIT)](http://www.victorpankratius.com)<br>
Project developer: [David Mascharka (MIT Lincoln Laboratory)](https://davidmascharka.com)<b><sup>[*](#note)</sup></b>

  
### Acknowledgements

We acknowledge support from NSF AGS-1343967 for the Mahali project and NSF AST-1156504 for the Research Experience for Undergraduates (REU) program.

## Examples
MCheetah was created as a reusable mobile framework for the NSF-sponsored Mahali project [http://mahali.mit.edu](http://mahali.mit.edu)

App examples using MCheetah: 

| App |  | 
| ------------- | ------------- |
| [MahaliRelayApp [Code]](https://github.com/MITHaystack/mcheetah/tree/master/MahaliRelayApp)| <ul><li><sup><b>Purpose:</b> Monitor Earth's ionosphere through GPS. </sup></li> <li><sup><b>Features:</b> Data acquisition from multifrequency GPS receivers, multithreaded RINEX file parsing on phone, bias removal, computation of ionospheric Total Electron Content (TEC), line of sight to vertical TEC tranformation through NASA satellite ephemeris access, visualization, upload to Dropbox. </sup></li> <li>[AGU2015 Abstract](https://github.com/MITHaystack/mcheetah/blob/master/presentations/MahaliRelayApp-AGU-2015.pdf)</li><br> <img alt="Screenshot" src="https://github.com/MITHaystack/mcheetah/blob/master/docs/images/screenshot_MahaliRelayApp.png"/> |
| [MagnetometerApp [Code]](https://github.com/MITHaystack/mcheetah/tree/master/MagnetometerApp)| <li><sup><b>Purpose:</b> App turning a phone into a mobile magnetometer.</sup></li><li><sup><b>Features:</b> Record data in nT, upload data to Dropbox.</sup></li> |

<a name="note"><sup>*</sup></a>: David is currently an MIT Lincoln Laboratory employee. No Laboratory funding or resources were used to produce the results here. This work was completed under the NSF REU program and the NSF Mahali project at MIT Haystack Observatory.
