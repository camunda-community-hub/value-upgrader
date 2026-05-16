// -----------------------------------------------------------
//
// Upgrader
//
// Manage the dashboard. Root component
//
// -----------------------------------------------------------

import React, {createRef} from 'react';
import {
    Accordion,
    AccordionItem,
    Button,
    CodeSnippet,
    FileUploaderDropContainer,
    FileUploaderItem,
    Select,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tag
} from "carbon-components-react";
import {ChevronRight} from "react-bootstrap-icons";


import ControllerPage from "../component/ControllerPage";
import RestCallService from "../services/RestCallService";


class Upgrader extends React.Component {


    constructor(_props) {
        super();
        this.fileUploaderRef = createRef();

        this.state = {
            dashboard: {
                details: [],
            },
            display: {
                version: "V87_88",
                openOutputYaml: false
            },
            files: [],
            result: {
                summary: "",
                outputYaml: "",
                reportEntries: []

            },
            status: ""
        };

    }

    componentDidMount() {
    }


    render() {
        const isOpen = this.state.display.openValueYaml;

        // console.log("dashboard.render display="+JSON.stringify(this.state.display));
        return (<div className={"container"}>
                <div className="row">
                    <div className="col-md-1">
                        <h1 className="title">Upgrader</h1>
                    </div>
                    <div className="row" style={{width: "100%"}}>
                        <div className="col-md-12">
                            <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>
                        </div>
                    </div>
                </div>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-6">
                        <p className="bx--label-description">Drag and drop a .yaml file here</p>
                        <FileUploaderDropContainer
                            labelText="Drag and drop a .yaml file or click to upload"
                            accept={['.yaml']}
                            multiple={false}
                            disabled={this.state.display.loading}
                            onAddFiles={(event, {addedFiles}) => this.handleDropFiles(addedFiles)}
                        />
                        {this.state.droppedFiles && this.state.droppedFiles.map((file) => (
                            <FileUploaderItem
                                key={file.name}
                                name={file.name}
                                status="edit"
                                iconDescription="Remove file"
                                onDelete={() => this.handleDropFileDelete(file.name)}
                            />
                        ))}

                        {this.state.statusUploadFailed &&
                            <div className="alert alert-danger" style={{
                                margin: "10px 10px 10px" +
                                    " 10px"
                            }}>
                                {this.state.statusUploadFailed}
                            </div>
                        }
                        {this.state.statusUploadSuccess && <div className="alert alert-success" style={{
                            margin: "10px 10px 10px" +
                                " 10px"
                        }}>
                            {this.state.statusUploadSuccess}
                        </div>}
                    </div>


                    <div className="col-md-6">
                        <Select
                            value={this.state.display.version}
                            labelText="Version"
                            disabled={this.state.display.loading}
                            onChange={(event) => this.setVersion(event.target.value)}>
                            <option value="V87_88">8.7 to 8.8</option>
                            <option value="V88_89">8.8 to 8.9</option>
                        </Select>
                    </div>
                </div>
                <div className="row" style={{width: "100%", paddingTop: "10px"}}>
                    <div className="col-md-12">
                        <Button onClick={() => this.convert()}
                                disabled={this.state.display.loading}>Convert</Button>

                    </div>
                </div>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <h2>Conversion</h2>
                        <span style={{
                            fontWeight: "bold",
                            fontSize: "0.8em",
                            margin: "5px 5px 5px 5px",
                            display: "block"
                        }}>
                        {this.state.result.summary}
                        </span>
                    </div>
                </div>


                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">

                        <Accordion>
                            <AccordionItem
                                key="value"
                                title={<strong>Value</strong>}
                                subtitle="Value upgraded"
                            >
                            {/* Content */}
                                <div style={{marginTop: 10}}>
                                    <CodeSnippet
                                        type="multi"
                                        feedback="Copied!"
                                        wrapText
                                    >
                                        {this.state.result.outputYaml}
                                    </CodeSnippet>

                                </div>
                            </AccordionItem>
                            <AccordionItem
                                key="resume"
                                title={<strong>Resume</strong>}
                                subtitle="Resule">
                                <Table>
                                    <TableHead>
                                        <TableRow>
                                            <TableHeader>Kind</TableHeader>
                                            <TableHeader>Rule Type</TableHeader>
                                            <TableHeader>Path</TableHeader>
                                            <TableHeader>Description</TableHeader>
                                            <TableHeader>Detail</TableHeader>
                                        </TableRow>
                                    </TableHead>

                                    <TableBody>
                                        {this.state.result && this.state.result.reportEntries && this.state.result.reportEntries.map((entry, index) => (
                                            <TableRow key={index}>
                                                <TableCell style={{minWidth: "120px", whiteSpace: "nowrap"}}>
                                                    <Tag type={this.getTagType(entry.kind)}>
                                                <span style={{fontSize: "0.75em", textTransform: "lowercase"}}>
                                                    {entry.kind}
                                                </span>
                                                    </Tag></TableCell>
                                                <TableCell style={{whiteSpace: "nowrap"}}>{entry.ruleType}</TableCell>
                                                <TableCell style={{whiteSpace: "nowrap"}}>{entry.path}</TableCell>
                                                <TableCell>{entry.description || "-"}</TableCell>
                                                <TableCell>{entry.detail}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </AccordionItem>
                        </Accordion>


                    </div>
                </div>



            </div>
        )

    }

    getTagType = (kind) => {
        switch (kind) {
            case "ERROR":
                return "red";
            case "WARNING":
                return "yellow";
            case "CHANGE":
                return "green";
            case "SKIP":
                return "blue";
            default:
                return "gray";
        }
    };

    toggleYaml() {
        console.log("toggle")
        this.setState((prev) => ({
            display: {
                ...prev.display,
                openValueYaml: !prev.display.openValueYaml,
            },
        }));
    };

    getButtonClass(active) {
        if (active)
            return "btn btn-primary btn-sm";
        return "btn btn-outline-primary btn-sm"
    }


    setVersion(value) {
        // console.log("DashBoard.setOrderBy: " + value);
        this.setDisplayProperty("version", value);
    }


    handleFileChange(event) {
        this.refreshStatusOnPage();
        const fileList = event.target.files;
        this.setState({files: fileList}); // Use spread operator to create a new array
    };

    handleDropFiles(addedFiles) {
        this.refreshStatusOnPage();
        const singleFile = [addedFiles[0]]; // keep only the first (replaces any previous)
        this.setState({droppedFiles: singleFile, files: singleFile});
    };

    handleDropFileDelete(fileName) {
        this.setState((prev) => {
            const updated = (prev.droppedFiles || []).filter((f) => f.name !== fileName);
            return {droppedFiles: updated, files: updated};
        });
    };

    convert(event) {
        console.log("Convert loadYaml " + this.state.files + " version [" + this.state.display.version);
        this.refreshStatusOnPage();
        let url = '/upgraded/api/v1/migratefile?version=' + this.state.display.version;
        console.log("URL: " + url);

        let restCallService = RestCallService.getInstance();

        const formData = new FormData();
        Array.from(this.state.files).forEach((file, index) => {
            formData.append(`File`, file);
        });
        /* formData.append("File", this.state.files[0]); */
        this.setDisplayProperty("loading", true);

        restCallService.postUpload(url, formData, this, this.convertCallback);


        // dispatch(connectorService.uploadJar(event.target.files[0]));
    }

    convertCallback(httpResponse) {
        console.log("loadYamlCallback start");
        this.setDisplayProperty("loading", false);

        if (httpResponse.isError()) {
            console.log("Upgrader.loadYamlCallback: error " + httpResponse.getError());
            this.setState({statusUploadFailed: httpResponse.getError()});
        } else {
            // Clear the file input field using JavaScript
            if (this.fileUploaderRef.current) {
                this.fileUploaderRef.current.clearFiles();
            }
            this.setState({'files': [], statusUploadSuccess: ''});
            this.setState({"result": httpResponse.getData()})
        }
    }

    /**
     * Set the display property
     * @param propertyName name of the property
     * @param propertyValue the value
     */
    setDisplayProperty(propertyName, propertyValue) {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }

    refreshStatusOnPage() {
        this.setState({statusUploadFailed: '', statusUploadSuccess: '', status: ''});
    }
}

export default Upgrader;
