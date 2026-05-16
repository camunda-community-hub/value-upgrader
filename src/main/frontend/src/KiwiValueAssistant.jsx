// -----------------------------------------------------------
//
// UpgradedValueApps
//
// Manage the main application
//
// -----------------------------------------------------------

import React from 'react';
import './index.scss';

import 'bootstrap/dist/css/bootstrap.min.css';

import {Container, Nav, Navbar} from 'react-bootstrap';
import Upgrader from "./upgrader/Upgrader";
import Rules from "./rules/Rules";


import Review from "./review/Review";
import ReviewNonDefault from "./review/ReviewNonDefault";
import HeaderMessage from "./HeaderMessage/HeaderMessage";

const FRAME_NAME = {
    UPGRADER: "Upgrader",
    RULES: "Rules",
    REVIEWSINGLEVALUE: "ReviewSingleValue",
    REVIEWNONDEFAULT: "ReviewNonDefault"

}

class KiwiValueAssistant extends React.Component {


    constructor(_props) {
        super();
        this.state = {frameContent: FRAME_NAME.UPGRADER};
        this.clickMenu = this.clickMenu.bind(this);
    }


    render() {
        return (
            <div>
                <Navbar bg="light" variant="light">
                    <Container>
                        <Nav className="mr-auto">
                            <Navbar.Brand href="#home">
                                    <div style={{display: "flex", flexDirection: "column", alignItems: "center"}}>
                                        <img src="/img/kiwi.png" width="28" height="28" alt="Kiwi"/>
                                        <span style={{
                                               fontSize: "8px",
                                               color: "#6f6f6f",
                                               textDecoration: "none",
                                               marginTop: "2px"
                                           }}>Image by muhammad.abdullah on Magnific
                                        </span>
                                    </div>
                            </Navbar.Brand>

                            <Nav.Link
                                active={this.state.frameContent === FRAME_NAME.UPGRADER}
                                onClick={() => {
                                    this.clickMenu(FRAME_NAME.UPGRADER)
                                }}>Upgrader</Nav.Link>

                            <Nav.Link
                                active={this.state.frameContent === FRAME_NAME.RULES}
                                onClick={() => {
                                    this.clickMenu(FRAME_NAME.RULES)
                                }}>Upgrader Rules</Nav.Link>

                            <Nav.Link
                                active={this.state.frameContent === FRAME_NAME.REVIEWSINGLEVALUE}
                                onClick={() => {
                                    this.clickMenu(FRAME_NAME.REVIEWSINGLEVALUE)
                                }}>Review Single value</Nav.Link>
                            <Nav.Link
                                active={this.state.frameContent === FRAME_NAME.REVIEWNONDEFAULT}
                                onClick={() => {
                                    this.clickMenu(FRAME_NAME.REVIEWNONDEFAULT)
                                }}>Review Get Non-defaults</Nav.Link>
                        </Nav>
                    </Container>
                </Navbar>
                <HeaderMessage/>
                {this.state.frameContent === FRAME_NAME.UPGRADER && <Upgrader/>}
                {this.state.frameContent === FRAME_NAME.RULES && <Rules/>}
                {this.state.frameContent === FRAME_NAME.REVIEWSINGLEVALUE && <Review/>}
                {this.state.frameContent === FRAME_NAME.REVIEWNONDEFAULT && <ReviewNonDefault/>}


            </div>);
    }


    clickMenu(menu) {
        console.log("ClickMenu " + menu);
        this.setState({frameContent: menu});

    }

}

export default KiwiValueAssistant;


