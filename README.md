# MCheetah
- Create data processing pipelines on mobile phones
- Easily connect your data I/O to cloud environments like Dropbox
- Skip detailed Android programming; just connect your pipeline stages using producer-consumer patterns.
- Create data processing fragment abstractions using 'Parsers', 'Computers', and 'Renderers'
- Leverage GPU-optimized plotting with OpenGL for large data sets
- Enjoy auto-tuning of threads that optimizes speed on different mobile architectures
- Enable approximate computing to optimize your app for speed, accuracy, or energy consumption. 
- Easily reuse your critical code for desktop Java applications
- Open spource (MIT license)

### Documentation

- Code documentation (JavaDoc): [/docs/index.html](https://github.com/MITHaystack/mcheetah/blob/master/docs/index.html)


### Contributors

Project lead: [Victor Pankratius (MIT)](http://www.victorpankratius.com)<br>
Project developers: [David Mascharka (MIT Lincoln Laboratory)](https://www.linkedin.com/in/david-mascharka-20999269)

  
### Acknowledgements

We acknowledge support from NSF AGS-1343967.

## Examples
MCheetah was created as a reusable mobile framework for the NSF-sponsored Mahali project [http://mahali.mit.edu](http://mahali.mit.edu)

App examples using MCheetah: 

| Android App Code |  | 
| ------------- | ------------- |
| [MahaliRelayApp](https://github.com/MITHaystack/mcheetah/tree/master/MahaliRelayApp)| <sup>Purpose: Monitor Earth's ionosphere through GPS. <br>Features: Data acquisition from multifrequency GPS receivers, multithreaded RINEX file parsing on phone, bias removal, computation of ionospheric Total Electron Content (TEC), line of sight to vertical TEC tranformation through NASA satellite ephemeris access, visualization, upload to Dropbox. </sup><img alt="Screenshot" src="https://github.com/MITHaystack/mcheetah/blob/master/docs/images/screenshot_MahaliRelayApp.png"/> |
| [MagnetometerApp](https://github.com/MITHaystack/mcheetah/tree/master/MagnetometerApp)| <sup>Purpose: App turning a phone into a mobile magnetometer.<br>Features: Record data in nT, upload data to Dropbox.</sup> |
