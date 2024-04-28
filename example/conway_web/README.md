# Conway Web UI Example

This is a React-based website that can be used in conjunction with `vdmj-remote` to
be hosted as a UI atop the [Conway.vdmsl](Conway.vdmsl) model. 

This UI functions by sending HTTP requests to the `/exec` endpoint of the running `vdmj-remote` 
instance to execute the next generation of the Game of Life model. This functionality
is defined in [src/backend-api.js](src/backend-api.js).

As long as it can be compiled to a static web page, any Node.js framework can be used.

## Compilation and use

To compile this for use with `vdmj-remote` first run

```commandline
npm install
```

Then run

```commandline
npm run build
```

Edit the [Conway.vdmsl](Conway.vdmsl) line 24 to reflect the path of the build
folder e.g.:

```
--@WebGUI("GUI", "/home/user/vdmj-remote/example/conway_web/build")
```

> Note: this is best as an absolute path as if you change where you start `vdmj-remote`
> it may not resolve otherwise.

Run the compiled `vdmj-remote` jar with the Conway.vdmsl script e.g.:

```commandline
java -jar /path/to/build/vdmj-remote-1.0-SNAPSHOT-shaded.jar -p 8080 -t vdmsl --sourcePath ./Conway.vdmsl
```


