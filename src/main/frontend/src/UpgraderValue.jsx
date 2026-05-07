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
import Dashboard from "./dashboard/Dashboard";
import Rules from "./rules/Rules";
import HeaderMessage from "./HeaderMessage/HeaderMessage";

const FRAME_NAME = {
    UPGRADER: "Upgrader",
    RULES: "Rules"

}

class UpgraderValue extends React.Component {


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
                                <img src="/img/cherries.png" width="28" height="28" alt="Upgraded Value"/>
                                Upgrader value
                            </Navbar.Brand>

                            <Nav.Link
                                active={this.state.frameContent  === FRAME_NAME.UPGRADER}
                                onClick={() => {
                                this.clickMenu(FRAME_NAME.UPGRADER)
                            }}>Upgrader</Nav.Link>

                            <Nav.Link
                                active={this.state.frameContent  === FRAME_NAME.RULES}
                                onClick={() => {
                                this.clickMenu(FRAME_NAME.RULES)
                            }}>Rules</Nav.Link>


                        </Nav>
                    </Container>
                </Navbar>
                <HeaderMessage/>
                {this.state.frameContent === FRAME_NAME.UPGRADER && <Dashboard/>}
                {this.state.frameContent === FRAME_NAME.RULES && <Rules/>}


            </div>);
    }


    clickMenu(menu) {
        console.log("ClickMenu " + menu);
        this.setState({frameContent: menu});

    }

}

export default UpgraderValue;


