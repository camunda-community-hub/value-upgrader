// -----------------------------------------------------------
//
// Rules
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import ControllerPage from "../component/ControllerPage";
import {
    Accordion,
    AccordionItem,
    Button,
    FileUploaderDropContainer,
    FileUploaderItem,
    Select,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tag,
    TextInput
} from "carbon-components-react";

import RestCallService from "../services/RestCallService";

// ── EN DEHORS de la classe ───────────────────────────────────────────────────
const LEVEL_TAG = {
    WARNING: "red",
    INFO: "blue",
    CLARIFICATION: "purple",
    ERROR: "high-contrast",
};

const LEVEL_ICON = {
    WARNING: "⚠",
    INFO: "ℹ",
    CLARIFICATION: "?",
};

// ── Classe principale ────────────────────────────────────────────────────────
class Review extends React.Component {

    constructor(_props) {
        super();
        this.state = {
            status: "",
            display: {
                version: "14.2.0",
                loading: false,
            },
            helmVersions: [],
            result: "",
            rules: [],
            isOpen: false
        };
    }

    componentDidMount() {
        this.fetchVersions();
    }

    render() {
        const groups = this.state.result?.rulesEvaluation ?? [];

        return (
            <div className={"container"}>
                <div className="row">
                    <div className="col-md-12">
                        <h1 className="title">Review Single Value</h1>
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
                            <div className="alert alert-danger" style={{margin: "10px 10px 10px 10px"}}>
                                {this.state.statusUploadFailed}
                            </div>
                        }
                        {this.state.statusUploadSuccess &&
                            <div className="alert alert-success" style={{margin: "10px 10px 10px 10px"}}>
                                {this.state.statusUploadSuccess}
                            </div>
                        }
                    </div>

                    <div className="col-md-6">
                        <TextInput
                            labelText="Helm version"
                            value={this.state.display.version}
                            onChange={(event) => this.setVersion(event.target.value)}>
                        </TextInput>
                        <Select
                            id="template-select"
                            labelText="Template"
                            onChange={(event) => this.setVersion(event.target.value)}
                        >
                            {this.state.helmVersions.map((version) => (
                                <option key={version} value={version}>{version}</option>
                            ))}
                        </Select>
                        <br/>
                        <a href="https://helm.camunda.io/camunda-platform/version-matrix/">Matrix</a>
                    </div>
                </div>

                <div className="row" style={{width: "100%", paddingTop: "10px"}}>
                    <div className="col-md-12">
                        <Button onClick={() => this.review()}
                                disabled={this.state.display.loading}>Review</Button>
                    </div>
                </div>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <h2>Result</h2>


                        {groups.length > 0 && (
                            <Accordion>
                                {groups.map((group) => (
                                    <AccordionItem
                                        key={group.id}
                                        title={<strong>{group.name}</strong>}
                                        subtitle={group.description}
                                    >
                                        <Table size="sm" useZebraStyles style={{width: "100%"}}>
                                            <TableHead>
                                                <TableRow>
                                                    <TableHeader>Level</TableHeader>
                                                    <TableHeader>Status</TableHeader>
                                                    <TableHeader>Comment</TableHeader>
                                                    <TableHeader>Expected value</TableHeader>
                                                </TableRow>
                                            </TableHead>
                                            <TableBody>
                                                {group.results.map((rule) => (
                                                    <TableRow key={rule.ruleName}>
                                                        <TableCell>
                                                            <Tag type={LEVEL_TAG[rule.level] ?? "gray"}>
                                                                <span style={{fontSize: "0.75em", textTransform: "lowercase",whiteSpace: "nowrap"}}>
                                                                    {LEVEL_ICON[rule.level]} {rule.level}
                                                                </span>
                                                            </Tag>


                                                        </TableCell>
                                                        <TableCell>
                                                            <Tag type={rule.followed ? "green" : "red"} size="sm">
                                                                 <span style={{fontSize: "0.75em", textTransform: "lowercase",whiteSpace: "nowrap"}}>
                                                                {rule.followed ? "✔ OK" : "✘ Missing"}
                                                                 </span>
                                                            </Tag>
                                                        </TableCell>
                                                        <TableCell>{rule.comment}</TableCell>
                                                        <TableCell>
                                                            <pre style={{fontSize: 11, margin: 0}}>
                                                                {rule.details?.expectedValue ?? rule.details?.unexpectedValue ?? "—"}
                                                            </pre>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </AccordionItem>
                                ))}
                            </Accordion>
                        )}
                    </div>
                </div>

            </div>
        );
    }

    setVersion(value) {
        this.setDisplayProperty("version", value);
    }

    handleFileChange(event) {
        this.refreshStatusOnPage();
        const fileList = event.target.files;
        this.setState({files: fileList});
    }

    handleDropFiles(addedFiles) {
        this.refreshStatusOnPage();
        const singleFile = [addedFiles[0]];
        this.setState({droppedFiles: singleFile, files: singleFile});
    }

    handleDropFileDelete(fileName) {
        this.setState((prev) => {
            const updated = (prev.droppedFiles || []).filter((f) => f.name !== fileName);
            return {droppedFiles: updated, files: updated};
        });
    }

    review() {
        console.log("review version [" + this.state.display.version);
        if (!this.state.files || this.state.files.length === 0) {
            this.setState({status: "Please upload a file to review", result: {}});
            return;
        }

        let url = '/review/api/v1/analysis?version=' + this.state.display.version;
        console.log("URL: " + url);

        let restCallService = RestCallService.getInstance();
        this.setDisplayProperty("loading", true);

        const formData = new FormData();
        Array.from(this.state.files).forEach((file) => {
            formData.append(`File`, file);
        });
        restCallService.postUpload(url, formData, this, this.reviewCallback);
    }

    reviewCallback(httpResponse) {
        console.log("loadRuleCallback start");
        this.setDisplayProperty("loading", false);

        if (httpResponse.isError()) {
            console.log("Rules.loadRuleCallback: error " + httpResponse.getError());
            this.setState({status: httpResponse.getError(), result: {}});
        } else {
            this.setState({status: "", result: httpResponse.getData()});
        }
    }

    fetchVersions() {
        console.log("FetchVersions ");
        let url = '/matrix/api/v1/versions?';
        console.log("URL: " + url);

        let restCallService = RestCallService.getInstance();
        this.setDisplayProperty("loading", true);
        restCallService.getJson(url, this, this.fetchVersionCallback);
    }

    fetchVersionCallback(httpResponse) {
        console.log("fetchVersionCallback start");
        this.setDisplayProperty("loading", false);

        if (httpResponse.isError()) {
            console.log("Rules.loadRuleCallback: error " + httpResponse.getError());
            this.setState({status: httpResponse.getError()});
        } else {
            this.setState({status: "", helmVersions: httpResponse.getData()});
        }
    }

    setDisplayProperty(propertyName, propertyValue) {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }

    refreshStatusOnPage() {
        this.setState({statusUploadFailed: '', statusUploadSuccess: '', status: ''});
    }
}

export default Review;
