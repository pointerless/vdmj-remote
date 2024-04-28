# VDMJ Remote

## An RPC system/REST API for [VDMJ](https://github.com/nickbattle/vdmj)

> This is currently WIP, commits may include errors

To compile run:

```commandline
mvn clean install
```

Use the JAR named:

`vdmj-extended-<version>-shaded.jar`

The CLI args are as follows:

```
--help, -h
  Print this help dialogue
--ipcAddress, -i
  Address to connect to for JSON communication with parent process, must 
  match '<hostname>:<port>'
--port, -p
  Port to bind on, if 0 random port will be used
  Default: 0
* --sourcePath, -s
  Source as a path, dir/files
* --type, -t
  Source type ([vdmrt, vdmsl, vdmpp])
```

For example:

```commandline
java -jar vdmj-remote-1.0-SNAPSHOT-shaded.jar -p 8080 -t vdmsl --sourcePath Conway.vdmsl
```

To embed a webpage atop a script, use the `@WebGUI(<nick>, <path>)` annotation, as demonstrated in
[example/conway_web/Conway.vdmsl](example/conway_web/Conway.vdmsl) replacing the path with the absolute
path to your static web content folder (e.g. `build` for Node or `www` for legacy web development).
