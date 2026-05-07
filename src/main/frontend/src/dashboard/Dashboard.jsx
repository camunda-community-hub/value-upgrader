// -----------------------------------------------------------
//
// Dashboard
//
// Manage the dashboard. Root component
//
// -----------------------------------------------------------

import React, {createRef} from 'react';
import {Button, FileUploader, Select, TextArea} from "carbon-components-react";
import {ChevronRight} from "react-bootstrap-icons";
import {Table,TableHead,TableRow,TableHeader,TableBody,TableCell, Tag} from "carbon-components-react";


import ControllerPage from "../component/ControllerPage";
import RestCallService from "../services/RestCallService";


class Dashboard extends React.Component {


    constructor(_props) {
        super();
        this.fileUploaderRef = createRef();

        this.state = {
            dashboard: {
                details: [],
            },
            display: {
                version: "87-to-88",
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

                        <FileUploader
                            ref={this.fileUploaderRef}
                            labelTitle="Upload YAML"
                            labelDescription="Only .yaml file"
                            buttonLabel="Add files"
                            filenameStatus="edit"
                            accept={['.yaml']}
                            onChange={(event) => this.handleFileChange(event)}
                            iconDescription="Clear file"
                            disabled={this.state.display.loading || this.state.files.size === 0}
                        />
                        <br/>

                        {this.state.statusUploadFailed && <div className="alert alert-danger" style={{
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
                            <option value="87-to-88">8.7 to 8.8</option>
                        </Select>
                    </div>
                </div>
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <Button onClick={() => this.loadYaml()}
                                disabled={this.state.display.loading}>Convert</Button>

                    </div>
                </div>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <h2>Conversion</h2>
                        this.state.result.summary
                    </div>
                </div>

                <div className="row" style={{width: "100%"}}>


                    <div className="row" style={{width: "100%"}}>
                        <div className="col-md-12">

                            <div style={{borderTop: "1px solid #d0d0d0", paddingTop: 12, borderRadius: 6, padding: 12}}>
                                {/* Header */}
                                <div
                                    onClick={() => this.toggleYaml()}
                                    style={{
                                        display: "flex",
                                        alignItems: "center",
                                        cursor: "pointer",
                                        userSelect: "none",
                                        fontWeight: 600,
                                    }}
                                >
                                    {/* Arrow */}
                                    <ChevronRight
                                        style={{
                                            transform: isOpen ? "rotate(90deg)" : "rotate(0deg)",
                                            transition: "0.2s"
                                        }}
                                    />
                                    "Value.yaml"
                                </div>

                                {/* Content */}
                                {isOpen && (
                                    <div style={{marginTop: 10}}>
                                        <TextArea
                                            value={this.state.result.outputYaml}
                                            readOnly
                                            readOnly
                                            labelText="YAML content"
                                            rows={15}
                                            style={{
                                                fontFamily: "monospace",
                                                border: "1px solid #8d8d8d",
                                                borderRadius: 4
                                            }}
                                        />

                                    </div>
                                )}
                            </div>


                        </div>
                    </div>

                    <div className="col-md-12">
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
                                        <TableCell>
                                            <Tag type={this.getTagType(entry.kind)}>
                                                {entry.kind}
                                            </Tag></TableCell>
                                        <TableCell>{entry.ruleType}</TableCell>
                                        <TableCell>{entry.path}</TableCell>
                                        <TableCell>{entry.description || "-"}</TableCell>
                                        <TableCell>{entry.detail}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
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

    loadYaml(event) {
        console.log("Load loadYaml ", this.state.files);
        this.refreshStatusOnPage();
        let restCallService = RestCallService.getInstance();

        const formData = new FormData();
        Array.from(this.state.files).forEach((file, index) => {
            formData.append(`File`, file);
        });
        /* formData.append("File", this.state.files[0]); */
        this.setDisplayProperty("loading", true);

        restCallService.postUpload('/upgraded/api/v1/migratefile?', formData, this, this.loadYamlCallback);


        // dispatch(connectorService.uploadJar(event.target.files[0]));
    }

    loadYamlCallback(httpResponse) {
        console.log("loadYamlCallback start");
        this.setDisplayProperty("loading", false);

        if (httpResponse.isError()) {
            console.log("Dashboard.loadYamlCallback: error " + httpResponse.getError());
            this.setState({statusUploadFailed: httpResponse.getError()});
        } else {
            // Clear the file input field using JavaScript
            if (this.fileUploaderRef.current) {
                this.fileUploaderRef.current.clearFiles();
            }
            this.setState({'files': [], statusUploadSuccess: ''});
        }
        this.setState({"result": httpResponse.getData()})
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

export default Dashboard;
